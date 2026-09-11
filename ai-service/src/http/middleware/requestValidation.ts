import {
  internalAiBuddyRequestSchema,
  internalShadowingRequestSchema,
  type InternalAiBuddyRequest,
  type InternalShadowingRequest,
} from "../../shared/contracts/internalRequest.js";
import { InternalServiceError } from "../../shared/contracts/errors.js";

export function parseAiBuddyRequest(rawBody: Buffer): InternalAiBuddyRequest {
  let parsed: unknown;
  try {
    parsed = JSON.parse(rawBody.toString("utf8"));
  } catch {
    throw new InternalServiceError("INVALID_INTERNAL_REQUEST", undefined, 400);
  }
  const result = internalAiBuddyRequestSchema.safeParse(parsed);
  if (!result.success) {
    throw new InternalServiceError("INVALID_INTERNAL_REQUEST", correlationIdOf(parsed), 400);
  }
  return result.data;
}

export function parseShadowingRequest(rawBody: Buffer): InternalShadowingRequest {
  let parsed: unknown;
  try {
    parsed = JSON.parse(rawBody.toString("utf8"));
  } catch {
    throw new InternalServiceError("INVALID_INTERNAL_REQUEST", undefined, 400);
  }
  const result = internalShadowingRequestSchema.safeParse(parsed);
  if (!result.success) {
    throw new InternalServiceError("INVALID_INTERNAL_REQUEST", correlationIdOf(parsed), 400);
  }
  return result.data;
}

function correlationIdOf(value: unknown): string | undefined {
  if (typeof value !== "object" || value === null || !("correlationId" in value)) return undefined;
  const correlationId = value.correlationId;
  return typeof correlationId === "string" ? correlationId : undefined;
}

