import { createServer, type IncomingMessage, type ServerResponse } from "node:http";

import { DeepSeekModelProvider } from "./adapters/llm/modelProvider.js";
import { loadEnvironment } from "./config/env.js";
import { AiBuddyService } from "./features/ai-buddy/aiBuddy.service.js";
import { ShadowingService } from "./features/shadowing/shadowing.service.js";
import { createMastra } from "./mastra/index.js";
import { writeInternalError } from "./http/middleware/errorHandler.js";
import { NonceReplayStore } from "./http/middleware/internalAuth.js";
import { handleInternalAiBuddyRequest } from "./http/routes/internalAiBuddy.route.js";
import { handleInternalShadowingRequest } from "./http/routes/internalShadowing.route.js";

const MAX_REQUEST_BYTES = 64 * 1024;
const environment = loadEnvironment();
const mastra = createMastra(environment);
const modelProvider = new DeepSeekModelProvider(environment);
const aiBuddyService = new AiBuddyService(modelProvider);
const shadowingService = new ShadowingService(modelProvider);
const replayStore = new NonceReplayStore();

const server = createServer(async (request, response) => {
  try {
    if (request.method === "POST" && request.url === "/internal/v1/ai-buddy/respond") {
      const body = await readBody(request);
      await handleInternalAiBuddyRequest(request.headers, body, response, { environment, replayStore, aiBuddyService });
      return;
    }
    if (request.method === "POST" && request.url === "/internal/v1/shadowing/assess") {
      const body = await readBody(request);
      await handleInternalShadowingRequest(request.headers, body, response, { environment, replayStore, shadowingService });
      return;
    }
    response.writeHead(404, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
    response.end(JSON.stringify({ code: "NOT_FOUND" }));
  } catch (error) {
    if (!response.headersSent) writeInternalError(response, error);
  }
});


server.listen(environment.port, environment.bindHost, () => {
  void mastra;
  process.stdout.write(`ai-service private listener started on ${environment.bindHost}:${environment.port}\n`);
});

for (const signal of ["SIGTERM", "SIGINT"] as const) {
  process.once(signal, () => {
    server.close(() => {
      void mastra.shutdown().finally(() => process.exit(0));
    });
  });
}

function readBody(request: IncomingMessage): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    let size = 0;
    request.on("data", (chunk: Buffer) => {
      size += chunk.length;
      if (size > MAX_REQUEST_BYTES) {
        reject(new Error("Request body exceeds limit."));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });
}
