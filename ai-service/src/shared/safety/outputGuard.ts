import type { InternalAiBuddyResponse } from "../contracts/internalResponse.js";

const UNSAFE_OUTPUT = /system\s+prompt|api\s*key|ignore\s+(previous|prior)\s+instructions|tự\s*tử|自杀|色情|khiêu\s*dâm/i;

export function isSafeAiBuddyOutput(response: InternalAiBuddyResponse): boolean {
  const combined = [response.chineseResponse, response.vietnameseExplanation, response.suggestion ?? ""].join("\n");
  return /[\u3400-\u9fff]/u.test(response.chineseResponse) && !UNSAFE_OUTPUT.test(combined);
}
