import type { RuntimeEnvironment } from "./env.js";

export interface ModelSelection {
  providerCode: "DEEPSEEK";
  modelCode: RuntimeEnvironment["deepseek"]["model"];
  transferRegion: string;
}

export function aiBuddyModelSelection(environment: RuntimeEnvironment): ModelSelection {
  return {
    providerCode: "DEEPSEEK",
    modelCode: environment.deepseek.model,
    transferRegion: environment.deepseek.transferRegion,
  };
}
