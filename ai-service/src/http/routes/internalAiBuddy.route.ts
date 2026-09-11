import type { IncomingHttpHeaders, ServerResponse } from "node:http";

import type { RuntimeEnvironment } from "../../config/env.js";
import { logSafeEvent } from "../../config/observability.js";
import { AiBuddyService } from "../../features/ai-buddy/aiBuddy.service.js";
import { NonceReplayStore, verifyInternalRequest } from "../middleware/internalAuth.js";
import { parseAiBuddyRequest } from "../middleware/requestValidation.js";

export interface InternalAiBuddyRouteDependencies {
  environment: RuntimeEnvironment;
  replayStore: NonceReplayStore;
  aiBuddyService: AiBuddyService;
}

export async function handleInternalAiBuddyRequest(
  headers: IncomingHttpHeaders,
  rawBody: Buffer,
  response: ServerResponse,
  dependencies: InternalAiBuddyRouteDependencies,
): Promise<void> {
  verifyInternalRequest(dependencies.environment, {
    timestamp: headerValue(headers, "x-internal-timestamp"),
    nonce: headerValue(headers, "x-internal-nonce"),
    contentSha256: headerValue(headers, "x-internal-content-sha256"),
    signature: headerValue(headers, "x-internal-signature"),
  }, rawBody, dependencies.replayStore);
  const request = parseAiBuddyRequest(rawBody);
  const result = await dependencies.aiBuddyService.respond(request);
  logSafeEvent({ event: "ai_buddy_request_succeeded", correlationId: result.correlationId });
  response.writeHead(200, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
  response.end(JSON.stringify(result));
}

function headerValue(headers: IncomingHttpHeaders, name: string): string | undefined {
  const value = headers[name];
  return Array.isArray(value) ? value[0] : value;
}
