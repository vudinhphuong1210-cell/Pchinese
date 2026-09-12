import { z } from "zod";

/**
 * Optional provider metering only. This contract intentionally excludes prompts, replies, audio,
 * JWTs, provider payloads and credentials.
 */
export const aiTelemetrySchema = z.strictObject({
  inputTokens: z.number().int().nonnegative().optional(),
  outputTokens: z.number().int().nonnegative().optional(),
  totalTokens: z.number().int().nonnegative().optional(),
  estimatedCost: z.number().nonnegative().optional(),
  costCurrency: z.string().regex(/^[A-Z]{3}$/).optional(),
  providerDurationMs: z.number().int().nonnegative().optional(),
  failureClass: z.enum(["timeout", "unavailable", "invalid-output", "safety-rejected"]).optional(),
}).superRefine((value, context) => {
  if ((value.estimatedCost === undefined) !== (value.costCurrency === undefined)) {
    context.addIssue({ code: z.ZodIssueCode.custom, message: "Cost and currency must be supplied together." });
  }
  if (value.totalTokens !== undefined && value.inputTokens !== undefined && value.outputTokens !== undefined
      && value.totalTokens !== value.inputTokens + value.outputTokens) {
    context.addIssue({ code: z.ZodIssueCode.custom, message: "Total tokens must equal input plus output tokens." });
  }
});

export type AiTelemetry = z.infer<typeof aiTelemetrySchema>;
