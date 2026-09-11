import { Agent } from "@mastra/core/agent";

import type { RuntimeEnvironment } from "../../config/env.js";

export function createChineseTutorAgent(environment: RuntimeEnvironment): Agent {
  return new Agent({
    id: "f11-chinese-tutor",
    name: "F11 Chinese Tutor",
    instructions: "Provide bounded Chinese-learning explanations only. Do not use memory, tools, or product data.",
    model: `deepseek/${environment.deepseek.model}`,
  });
}
