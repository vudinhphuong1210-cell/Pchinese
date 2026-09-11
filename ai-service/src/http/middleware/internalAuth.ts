import { createHash, createHmac, timingSafeEqual } from "node:crypto";

import type { RuntimeEnvironment } from "../../config/env.js";
import { InternalServiceError } from "../../shared/contracts/errors.js";

const MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export interface InternalHeaders {
  timestamp?: string | undefined;
  nonce?: string | undefined;
  contentSha256?: string | undefined;
  signature?: string | undefined;
}

export class NonceReplayStore {
  private readonly entries = new Map<string, number>();

  consume(nonce: string, now: number): boolean {
    this.removeExpired(now);
    if (this.entries.has(nonce)) return false;
    this.entries.set(nonce, now + MAX_CLOCK_SKEW_MS);
    return true;
  }

  private removeExpired(now: number): void {
    for (const [nonce, expiresAt] of this.entries) {
      if (expiresAt <= now) this.entries.delete(nonce);
    }
  }
}

export function verifyInternalRequest(
  environment: RuntimeEnvironment,
  headers: InternalHeaders,
  rawBody: Buffer,
  replayStore: NonceReplayStore,
  now: number = Date.now(),
): void {
  const timestamp = headers.timestamp;
  const nonce = headers.nonce;
  const contentSha256 = headers.contentSha256;
  const signature = headers.signature;
  if (!timestamp || !nonce || !contentSha256 || !signature || !UUID_PATTERN.test(nonce)
      || !/^[a-f0-9]{64}$/.test(contentSha256) || !/^[a-f0-9]{64}$/.test(signature)) {
    throw invalidRequest();
  }

  const parsedTime = Date.parse(timestamp);
  if (!Number.isFinite(parsedTime) || Math.abs(now - parsedTime) > MAX_CLOCK_SKEW_MS) {
    throw invalidRequest();
  }

  const calculatedHash = createHash("sha256").update(rawBody).digest("hex");
  if (!constantTimeEquals(calculatedHash, contentSha256)) {
    throw invalidRequest();
  }

  const canonical = ["POST", "/internal/v1/ai-buddy/respond", timestamp, nonce, contentSha256].join("\n");
  const expectedSignature = createHmac("sha256", environment.internalHmacSecret).update(canonical, "utf8").digest("hex");
  if (!constantTimeEquals(expectedSignature, signature) || !replayStore.consume(nonce, now)) {
    throw invalidRequest();
  }
}

function constantTimeEquals(left: string, right: string): boolean {
  const leftBytes = Buffer.from(left, "utf8");
  const rightBytes = Buffer.from(right, "utf8");
  return leftBytes.length === rightBytes.length && timingSafeEqual(leftBytes, rightBytes);
}

function invalidRequest(): InternalServiceError {
  return new InternalServiceError("INVALID_INTERNAL_REQUEST", undefined, 400);
}
