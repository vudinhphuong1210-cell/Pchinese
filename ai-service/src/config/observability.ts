export interface SafeEvent {
  event: string;
  correlationId?: string | undefined;
  code?: string | undefined;
}

export function logSafeEvent(event: SafeEvent): void {
  const fields = ["event=" + event.event];
  if (event.correlationId) fields.push("correlationId=" + event.correlationId);
  if (event.code) fields.push("code=" + event.code);
  process.stdout.write(fields.join(" ") + "\n");
}
