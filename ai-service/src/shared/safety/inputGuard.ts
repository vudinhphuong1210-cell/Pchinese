import type { InternalAiBuddyRequest } from "../contracts/internalRequest.js";

const UNSAFE_PATTERNS: readonly RegExp[] = [
  /ignore\s+(all\s+)?(previous|prior)\s+instructions/i,
  /system\s+prompt|reveal\s+(the\s+)?prompt/i,
  /bỏ\s+qua.*hướng\s+dẫn|tiết\s+lộ.*(prompt|hướng\s+dẫn)/i,
  /忽略.*(指令|提示)|泄露.*(提示|系统)/u,
  /\b[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}\b/,
  /\b(?:\+?84|0)\d{8,10}\b/,
  /suicide|tự\s*tử|自杀|kill\s+(?:myself|someone)|giết\s*(?:mình|người)/i,
  /porn|sexual|khiêu\s*dâm|色情|sex\b/i,
  /bomb|weapon|ma túy|drugs?|炸弹|武器/i,
];

const LEARNING_MARKERS = /[\u3400-\u9fff]|tiếng\s*trung|trung\s*quốc|pinyin|từ\s*vựng|ngữ\s*pháp|phát\s*âm|hán\s*tự|中文|汉语|拼音|词汇|语法/u;

export function isSafeAiBuddyInput(request: InternalAiBuddyRequest): boolean {
  const combined = [request.currentMessage, ...request.context.map((message) => message.content)].join("\n");
  return LEARNING_MARKERS.test(request.currentMessage) && !UNSAFE_PATTERNS.some((pattern) => pattern.test(combined));
}
