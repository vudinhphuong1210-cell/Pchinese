import type { IncomingHttpHeaders, ServerResponse } from "node:http";

import type { RuntimeEnvironment } from "../../config/env.js";
import { logSafeEvent } from "../../config/observability.js";
import { ShadowingService } from "../../features/shadowing/shadowing.service.js";
import { NonceReplayStore, verifyInternalRequest } from "../middleware/internalAuth.js";
import { parseShadowingRequest } from "../middleware/requestValidation.js";

export interface InternalShadowingRouteDependencies {
  environment: RuntimeEnvironment;
  replayStore: NonceReplayStore;
  shadowingService: ShadowingService;
}

export async function handleInternalShadowingRequest(
  headers: IncomingHttpHeaders,
  rawBody: Buffer,
  response: ServerResponse,
  dependencies: InternalShadowingRouteDependencies,
): Promise<void> {
  verifyInternalRequest(
    dependencies.environment,
    {
      timestamp: headerValue(headers, "x-internal-timestamp"),
      nonce: headerValue(headers, "x-internal-nonce"),
      contentSha256: headerValue(headers, "x-internal-content-sha256"),
      signature: headerValue(headers, "x-internal-signature"),
    },
    rawBody,
    dependencies.replayStore,
  );
  const request = parseShadowingRequest(rawBody);
  const result = await dependencies.shadowingService.assess(request);
  logSafeEvent({ event: "shadowing_assessment_succeeded", correlationId: result.correlationId });
  response.writeHead(200, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
  response.end(JSON.stringify(result));
}

function headerValue(headers: IncomingHttpHeaders, name: string): string | undefined {
  const value = headers[name];
  return Array.isArray(value) ? value[0] : value;
}
