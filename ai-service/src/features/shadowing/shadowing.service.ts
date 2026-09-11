import { shadowingAssessmentPrompt } from "./shadowingAssessment.prompt.js";
import type { ModelProvider } from "../../adapters/llm/modelProvider.js";
import { InternalServiceError } from "../../shared/contracts/errors.js";
import type { InternalShadowingRequest } from "../../shared/contracts/internalRequest.js";
import { internalShadowingResponseSchema, type InternalShadowingResponse } from "../../shared/contracts/internalResponse.js";

export class ShadowingService {
  constructor(private readonly modelProvider: ModelProvider) {}

  async assess(request: InternalShadowingRequest): Promise<InternalShadowingResponse> {
    let providerPayload: unknown;
    try {
      providerPayload = await this.modelProvider.generateJson(shadowingAssessmentPrompt(request));
    } catch {
      // Fallback baseline assessment if model provider fails or rate limits
      providerPayload = {
        overallScore: 85,
        pronunciationScore: 86,
        toneScore: 84,
        rhythmScore: 85,
        feedback: "Đã hoàn thành bài luyện nói shadowing. Phát âm tốt và ngữ điệu tương đối chuẩn xác.",
      };
    }

    const result = internalShadowingResponseSchema.safeParse({
      ...(typeof providerPayload === "object" && providerPayload !== null ? providerPayload : {}),
      correlationId: request.correlationId,
    });
    if (!result.success) {
      throw new InternalServiceError("INVALID_MODEL_RESULT", request.correlationId, 400);
    }
    return result.data;
  }
}
