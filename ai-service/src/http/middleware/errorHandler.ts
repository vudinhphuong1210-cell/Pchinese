import type { ServerResponse } from "node:http";

import { logSafeEvent } from "../../config/observability.js";
import { InternalServiceError, safeErrorBody } from "../../shared/contracts/errors.js";

export function writeInternalError(response: ServerResponse, error: unknown): void {
  const safeError = error instanceof InternalServiceError
    ? error
    : new InternalServiceError("PROVIDER_UNAVAILABLE", undefined, 503);
  logSafeEvent({ event: "ai_buddy_request_failed", correlationId: safeError.correlationId, code: safeError.code });
  response.writeHead(safeError.status, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
  response.end(JSON.stringify(safeErrorBody(safeError)));
}
