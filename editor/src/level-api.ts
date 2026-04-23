import { LevelDocument } from "./level-types";

export async function fetchLevelFileNames(): Promise<string[]> {
  return requestJson<string[]>("/api/levels");
}

export async function fetchLevelDocument(fileName: string): Promise<LevelDocument> {
  return requestJson<LevelDocument>(`/api/levels/${encodeURIComponent(fileName)}`);
}

export async function saveLevelDocument(fileName: string, level: LevelDocument): Promise<void> {
  await request(`/api/levels/${encodeURIComponent(fileName)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(level, null, 2)
  });
}

export async function deleteLevelDocument(fileName: string): Promise<void> {
  await request(`/api/levels/${encodeURIComponent(fileName)}`, {
    method: "DELETE"
  });
}

async function requestJson<T>(url: string): Promise<T> {
  const response = await request(url);
  return (await response.json()) as T;
}

async function request(url: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(url, init);
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return response;
}
