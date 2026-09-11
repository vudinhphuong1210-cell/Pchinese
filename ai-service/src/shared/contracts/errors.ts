export type InternalErrorCode =
  | "INVALID_INTERNAL_REQUEST"
  | "INPUT_SAFETY_REJECTED"
  | "INVALID_MODEL_RESULT"
  | "OUTPUT_SAFETY_REJECTED"
  | "PROVIDER_UNAVAILABLE";

export class InternalServiceError extends Error {
  constructor(
    public readonly code: InternalErrorCode,
    public readonly correlationId: string | undefined,
    public readonly status: 400 | 503,
  ) {
    super(code);
  }
}

export function safeErrorBody(error: InternalServiceError): Record<string, string> {
  return error.correlationId
    ? { code: error.code, correlationId: error.correlationId }
    : { code: error.code, correlationId: "00000000-0000-0000-0000-000000000000" };
}
