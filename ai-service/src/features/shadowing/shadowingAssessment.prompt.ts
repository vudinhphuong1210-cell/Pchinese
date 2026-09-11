import type { InternalShadowingRequest } from "../../shared/contracts/internalRequest.js";

export function shadowingAssessmentPrompt(request: InternalShadowingRequest): string {
  return [
    "You are an expert Chinese pronunciation and shadowing coach evaluating a Vietnamese learner's shadowing attempt.",
    "Evaluate the pronunciation, tones, and rhythm based on the target text.",
    `Target Chinese text (Hanzi): ${request.expectedHanzi}`,
    request.expectedPinyin ? `Target Pinyin: ${request.expectedPinyin}` : "",
    "Return JSON only with the following numeric fields (from 0 to 100) and a concise feedback string in Vietnamese:",
    JSON.stringify(
      {
        overallScore: 85,
        pronunciationScore: 88,
        toneScore: 82,
        rhythmScore: 85,
        feedback: "Phát âm rất rõ ràng, thanh điệu tự nhiên. Chú ý uốn lưỡi nhẹ hơn ở các âm zh, ch.",
      },
      null,
      2,
    ),
    "Do not output markdown code blocks or text outside JSON.",
  ]
    .filter(Boolean)
    .join("\n\n");
}
