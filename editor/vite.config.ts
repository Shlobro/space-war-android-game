import fs from "node:fs/promises";
import path from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const levelsDirectory = path.resolve(__dirname, "../app/src/main/assets/levels");

export default defineConfig({
  plugins: [
    react(),
    {
      name: "packaged-levels-api",
      configureServer(server) {
        server.middlewares.use("/api/levels", async (req, res) => {
          try {
            const fileName = sanitizeFileName(req.url ?? "");

            if (req.method === "GET" && !fileName) {
              const entries = await fs.readdir(levelsDirectory, { withFileTypes: true });
              const names = entries
                .filter((entry) => entry.isFile() && entry.name.endsWith(".json"))
                .map((entry) => entry.name)
                .sort();
              return sendJson(res, 200, names);
            }

            if (!fileName) {
              return sendText(res, 400, "A level file name is required.");
            }

            const filePath = path.join(levelsDirectory, fileName);

            if (req.method === "GET") {
              const content = await fs.readFile(filePath, "utf8");
              return sendJson(res, 200, JSON.parse(content));
            }

            if (req.method === "PUT") {
              const body = await readRequestBody(req);
              await fs.writeFile(filePath, `${body.trimEnd()}\n`, "utf8");
              return sendJson(res, 200, { ok: true });
            }

            if (req.method === "DELETE") {
              await fs.unlink(filePath);
              return sendJson(res, 200, { ok: true });
            }

            return sendText(res, 405, "Method not allowed.");
          } catch (error) {
            return sendText(res, 500, error instanceof Error ? error.message : "Levels API error.");
          }
        });
      }
    }
  ]
});

function sanitizeFileName(urlPath: string): string | null {
  const cleaned = decodeURIComponent(urlPath.replace(/^\/+/, ""));
  if (!cleaned) {
    return null;
  }
  if (!cleaned.endsWith(".json") || cleaned.includes("/") || cleaned.includes("\\") || cleaned.includes("..")) {
    throw new Error("Invalid level file name.");
  }
  return cleaned;
}

function readRequestBody(request: NodeJS.ReadableStream): Promise<string> {
  return new Promise((resolve, reject) => {
    let body = "";
    request.setEncoding("utf8");
    request.on("data", (chunk) => {
      body += chunk;
    });
    request.on("end", () => resolve(body));
    request.on("error", reject);
  });
}

function sendJson(response: { setHeader: (name: string, value: string) => void; end: (body: string) => void; statusCode: number }, statusCode: number, value: unknown): void {
  response.statusCode = statusCode;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.end(JSON.stringify(value));
}

function sendText(response: { setHeader: (name: string, value: string) => void; end: (body: string) => void; statusCode: number }, statusCode: number, value: string): void {
  response.statusCode = statusCode;
  response.setHeader("Content-Type", "text/plain; charset=utf-8");
  response.end(value);
}
