import { z } from "zod";

export const internalAiBuddyResponseSchema = z.strictObject({
  correlationId: z.uuid(),
  chineseResponse: z.string().trim().min(1).max(1000),
  vietnameseExplanation: z.string().trim().min(1).max(1000),
  suggestion: z.string().trim().min(1).max(500).nullable(),
});

export type InternalAiBuddyResponse = z.infer<typeof internalAiBuddyResponseSchema>;
