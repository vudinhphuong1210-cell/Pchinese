import { createDeepSeek } from "@ai-sdk/deepseek";
import { generateText } from "ai";

import type { RuntimeEnvironment } from "../../config/env.js";

export interface ModelProvider {
  generateJson(prompt: string): Promise<unknown>;
}

export class DeepSeekModelProvider implements ModelProvider {
  private readonly provider;

  constructor(private readonly environment: RuntimeEnvironment) {
    this.provider = createDeepSeek({ apiKey: environment.deepseek.apiKey });
  }

  async generateJson(prompt: string): Promise<unknown> {
    const result = await generateText({
      model: this.provider(this.environment.deepseek.model),
      prompt,
      maxRetries: 0,
      abortSignal: AbortSignal.timeout(22_000),
      temperature: 0.2,
    });
    try {
      return JSON.parse(result.text);
    } catch {
      throw new Error("Provider returned malformed structured output.");
    }
  }
}
