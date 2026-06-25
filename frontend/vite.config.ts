import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import type { IncomingMessage } from "node:http";

const backendBaseUrl = "http://127.0.0.1:8081/StudentAnalytics";

function readRequestBody(req: IncomingMessage) {
  return new Promise<Buffer>((resolve, reject) => {
    const chunks: Buffer[] = [];
    req.on("data", (chunk: Buffer) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

export default defineConfig({
  plugins: [
    vue(),
    {
      name: "student-analytics-api-proxy",
      configureServer(server) {
        server.middlewares.use("/api", async (req, res) => {
          try {
            const originalUrl = req.originalUrl || req.url || "";
            const targetPath = originalUrl.replace(/^\/api/, "");
            const targetUrl = `${backendBaseUrl}${targetPath}`;
            const headers = new Headers();
            for (const [key, value] of Object.entries(req.headers)) {
              if (!value || ["host", "connection", "content-length"].includes(key.toLowerCase())) {
                continue;
              }
              headers.set(key, Array.isArray(value) ? value.join(",") : value);
            }

            const method = req.method || "GET";
            const body = method === "GET" || method === "HEAD" ? undefined : await readRequestBody(req);
            const response = await fetch(targetUrl, { method, headers, body });
            res.statusCode = response.status;
            response.headers.forEach((value, key) => {
              if (!["content-encoding", "transfer-encoding"].includes(key.toLowerCase())) {
                res.setHeader(key, value);
              }
            });
            res.end(Buffer.from(await response.arrayBuffer()));
          } catch (error) {
            res.statusCode = 502;
            res.setHeader("content-type", "application/json");
            res.end(JSON.stringify({ code: 1, message: error instanceof Error ? error.message : "API proxy failed" }));
          }
        });
      }
    }
  ],
  server: {
    port: 5173
  }
});
