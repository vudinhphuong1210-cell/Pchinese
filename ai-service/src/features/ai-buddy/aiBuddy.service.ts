import { chineseTutorPrompt } from "./chineseTutor.prompt.js";
import type { ModelProvider } from "../../adapters/llm/modelProvider.js";
import { InternalServiceError } from "../../shared/contracts/errors.js";
import type { InternalAiBuddyRequest } from "../../shared/contracts/internalRequest.js";
import { internalAiBuddyResponseSchema, type InternalAiBuddyResponse } from "../../shared/contracts/internalResponse.js";
import { isSafeAiBuddyInput } from "../../shared/safety/inputGuard.js";
import { isSafeAiBuddyOutput } from "../../shared/safety/outputGuard.js";

export class AiBuddyService {
  constructor(private readonly modelProvider: ModelProvider) {}

  async respond(request: InternalAiBuddyRequest): Promise<InternalAiBuddyResponse> {
    if (!isSafeAiBuddyInput(request)) {
      throw new InternalServiceError("INPUT_SAFETY_REJECTED", request.correlationId, 400);
    }
    let providerPayload: unknown;
    try {
      providerPayload = await this.modelProvider.generateJson(chineseTutorPrompt(request));
    } catch {
      throw new InternalServiceError("PROVIDER_UNAVAILABLE", request.correlationId, 503);
    }

    const result = internalAiBuddyResponseSchema.safeParse({
      ...(typeof providerPayload === "object" && providerPayload !== null ? providerPayload : {}),
      correlationId: request.correlationId,
    });
    if (!result.success) {
      throw new InternalServiceError("INVALID_MODEL_RESULT", request.correlationId, 400);
    }
    if (!isSafeAiBuddyOutput(result.data)) {
      throw new InternalServiceError("OUTPUT_SAFETY_REJECTED", request.correlationId, 400);
    }
    return result.data;
  }
}
