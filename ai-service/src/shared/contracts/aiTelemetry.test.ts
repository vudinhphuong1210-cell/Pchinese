import { describe, expect, test } from "vitest";

import { aiTelemetrySchema } from "./aiTelemetry.js";

describe("F12 private telemetry contract", () => {
  test("accepts optional metering and preserves measured zero", () => {
    const result = aiTelemetrySchema.safeParse({ inputTokens: 0, outputTokens: 0, totalTokens: 0, estimatedCost: 0, costCurrency: "USD" });
    expect(result.success).toBe(true);
  });

  test("rejects unpaired, inconsistent, or unsafe telemetry", () => {
    expect(aiTelemetrySchema.safeParse({ estimatedCost: 1 }).success).toBe(false);
    expect(aiTelemetrySchema.safeParse({ inputTokens: 2, outputTokens: 3, totalTokens: 6 }).success).toBe(false);
    expect(aiTelemetrySchema.safeParse({ failureClass: "provider-body" }).success).toBe(false);
  });
});
