export type DeepSeekModel = "deepseek-v4-flash" | "deepseek-v4-pro";

export interface RuntimeEnvironment {
  bindHost: string;
  port: number;
  internalHmacSecret: string;
  deepseek: {
    apiKey: string;
    model: DeepSeekModel;
    transferRegion: string;
  };
}

const ALLOWED_MODELS: ReadonlySet<string> = new Set(["deepseek-v4-flash", "deepseek-v4-pro"]);
const PRIVATE_BIND_HOSTS: ReadonlySet<string> = new Set(["127.0.0.1", "::1", "0.0.0.0"]);

export function loadEnvironment(source: NodeJS.ProcessEnv = process.env): RuntimeEnvironment {
  const bindHost = required(source, "AI_SERVICE_BIND_HOST");
  if (!PRIVATE_BIND_HOSTS.has(bindHost)) {
    throw new Error("AI_SERVICE_BIND_HOST must bind to an approved private-network listener.");
  }
  if (source.AI_SERVICE_PRIVATE_NETWORK_ONLY !== "true") {
    throw new Error("AI_SERVICE_PRIVATE_NETWORK_ONLY must be true.");
  }

  const port = Number(required(source, "AI_SERVICE_PORT"));
  if (!Number.isSafeInteger(port) || port < 1 || port > 65535) {
    throw new Error("AI_SERVICE_PORT must be a valid TCP port.");
  }

  const model = required(source, "DEEPSEEK_MODEL");
  if (!ALLOWED_MODELS.has(model)) {
    throw new Error("DEEPSEEK_MODEL must be deepseek-v4-flash or deepseek-v4-pro.");
  }
  if (source.DEEPSEEK_DPA_APPROVED !== "true" || source.DEEPSEEK_NO_TRAINING !== "true"
      || source.DEEPSEEK_DELETION_ATTESTATION !== "true" || source.DEEPSEEK_MAX_RETENTION_HOURS !== "24") {
    throw new Error("DeepSeek privacy policy gate is incomplete.");
  }

  return {
    bindHost,
    port,
    internalHmacSecret: requiredSecret(source, "INTERNAL_HMAC_SECRET"),
    deepseek: {
      apiKey: requiredSecret(source, "DEEPSEEK_API_KEY"),
      model: model as DeepSeekModel,
      transferRegion: required(source, "DEEPSEEK_TRANSFER_REGION"),
    },
  };
}

function required(source: NodeJS.ProcessEnv, key: string): string {
  const value = source[key]?.trim();
  if (!value) {
    throw new Error(`${key} is required.`);
  }
  return value;
}

function requiredSecret(source: NodeJS.ProcessEnv, key: string): string {
  const value = required(source, key);
  if (value.includes("replace-with")) {
    throw new Error(`${key} must be supplied by the deployment secret store.`);
  }
  return value;
}
