import { z } from "zod";

export const internalAiBuddyResponseSchema = z.strictObject({
  correlationId: z.uuid(),
  chineseResponse: z.string().trim().min(1).max(1000),
  vietnameseExplanation: z.string().trim().min(1).max(1000),
  suggestion: z.string().trim().min(1).max(500).nullable(),
});

export const internalShadowingResponseSchema = z.strictObject({
  correlationId: z.uuid(),
  overallScore: z.number().min(0).max(100),
  pronunciationScore: z.number().min(0).max(100),
  toneScore: z.number().min(0).max(100),
  rhythmScore: z.number().min(0).max(100),
  feedback: z.string().trim().min(1).max(2000),
});

export type InternalAiBuddyResponse = z.infer<typeof internalAiBuddyResponseSchema>;
export type InternalShadowingResponse = z.infer<typeof internalShadowingResponseSchema>;

