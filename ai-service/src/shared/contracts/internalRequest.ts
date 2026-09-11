import { z } from "zod";

export const scenarioSchema = z.enum(["DAILY_CONVERSATION", "VOCABULARY_GRAMMAR", "ROLE_PLAY"]);
export const contextMessageSchema = z.strictObject({
  sender: z.enum(["LEARNER", "ASSISTANT"]),
  content: z.string().trim().min(1).max(1000),
});
export const internalAiBuddyRequestSchema = z.strictObject({
  correlationId: z.uuid(),
  requestId: z.uuid(),
  capability: z.literal("AI_BUDDY"),
  scenario: scenarioSchema,
  currentMessage: z.string().trim().min(1).max(1000),
  context: z.array(contextMessageSchema).max(10),
});

export const internalShadowingRequestSchema = z.strictObject({
  correlationId: z.uuid(),
  requestId: z.uuid(),
  segmentId: z.uuid(),
  expectedHanzi: z.string().trim().min(1).max(500),
  expectedPinyin: z.string().trim().max(500).nullable().optional(),
  recordingId: z.uuid(),
  audioBase64: z.string().nullable().optional(),
});

export type Scenario = z.infer<typeof scenarioSchema>;
export type ContextMessage = z.infer<typeof contextMessageSchema>;
export type InternalAiBuddyRequest = z.infer<typeof internalAiBuddyRequestSchema>;
export type InternalShadowingRequest = z.infer<typeof internalShadowingRequestSchema>;

