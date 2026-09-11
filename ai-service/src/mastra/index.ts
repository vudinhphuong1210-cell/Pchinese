import { Mastra } from "@mastra/core/mastra";

import type { RuntimeEnvironment } from "../config/env.js";
import { createChineseTutorAgent } from "../features/ai-buddy/chineseTutor.agent.js";

export function createMastra(environment: RuntimeEnvironment): Mastra {
  return new Mastra({
    agents: { chineseTutor: createChineseTutorAgent(environment) },
    logger: false,
  });
}
