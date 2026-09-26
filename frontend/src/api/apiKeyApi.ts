import { apiClient } from "./client";
import { apiKeys as seedApiKeys, SpringPage, type ApiKey } from "@/lib/mock-data";

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";
const STORAGE_KEY = "hook_shuttle_api_keys";

function getLocalApiKeys(): ApiKey[] {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seedApiKeys));
    return seedApiKeys;
  }
  return JSON.parse(stored);
}

function saveLocalApiKeys(keys: ApiKey[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(keys));
}

export async function fetchApiKeys(page: number, size: number): Promise<SpringPage<ApiKey>> {
  if (USE_MOCK) {
    const all = getLocalApiKeys();
    const start = page * size;
    const content = all.slice(start, start + size);

    return {
      content,
      total_elements: all.length,
      total_pages: Math.ceil(all.length / size),
      number: page,
      size,
    };
  }
  return apiClient.get(`/api/v1/api-keys?page=${page}&size=${size}`);
}
export async function createApiKey(data: {
  key_name: string;
  live: boolean;
}): Promise<ApiKey & { api_key: string }> {
  if (USE_MOCK) {
    const keys = getLocalApiKeys();
    const chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    const prefix = data.live ? "hs_live_" : "hs_test_";
    let randomStr = "";
    for (let i = 0; i < 32; i++) randomStr += chars[Math.floor(Math.random() * chars.length)];
    const api_key = `${prefix}${randomStr}`;

    const created: ApiKey = {
      id: `ak_${Math.random().toString(16).slice(2, 10)}`,
      key_name: data.key_name,
      key_prefix: api_key.slice(0, 12),
      status: "ACTIVE",
      created_at: new Date().toISOString(),
      last_used_at: null,
    };
    saveLocalApiKeys([created, ...keys]);
    return { ...created, api_key };
  }
  return apiClient.post("/api/v1/api-keys", data);
}

export async function revokeApiKey(id: string): Promise<void> {
  if (USE_MOCK) {
    const keys = getLocalApiKeys();
    const updated = keys.map((k) => (k.id === id ? { ...k, status: "REVOKED" as const } : k));
    saveLocalApiKeys(updated);
    return;
  }
  return apiClient.delete(`/api/v1/api-keys/${id}`);
}
