import { apiClient } from "./client";
import { endpoints as seedEndpoints, type Endpoint } from "@/lib/mock-data";

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";
const STORAGE_KEY = "hook_shuttle_endpoints";

function getLocalEndpoints(): Endpoint[] {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seedEndpoints));
    return seedEndpoints;
  }
  return JSON.parse(stored);
}

function saveLocalEndpoints(endpoints: Endpoint[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(endpoints));
}

export async function fetchEndpoints(): Promise<Endpoint[]> {
  if (USE_MOCK) {
    return getLocalEndpoints();
  }
  return apiClient.get("/api/v1/endpoints");
}

export async function createEndpoint(data: {
  target_url: string;
  description: string;
  rate_limit_per_sec: number;
  timeout_ms: number;
  max_retries: number;
  active: boolean;
}): Promise<Endpoint> {
  if (USE_MOCK) {
    const endpoints = getLocalEndpoints();
    const chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    let out = "";
    for (let i = 0; i < 22; i++) out += chars[Math.floor(Math.random() * chars.length)];
    
    const created: Endpoint = {
      id: `ep_${Math.random().toString(16).slice(2, 10)}`,
      target_url: data.target_url,
      description: data.description || "No description",
      secret_key: `whsec_${out}`,
      status: data.active ? "ACTIVE" : "DISABLED",
      rate_limit_per_sec: data.rate_limit_per_sec,
      timeout_ms: data.timeout_ms,
      max_retries: data.max_retries,
    };
    saveLocalEndpoints([created, ...endpoints]);
    return created;
  }
  return apiClient.post("/api/v1/endpoints", data);
}

export async function toggleEndpointStatus(id: string): Promise<void> {
  if (USE_MOCK) {
    const endpoints = getLocalEndpoints();
    let updatedEndpoint: Endpoint | null = null;
    const updated = endpoints.map((r) => {
      if (r.id === id) {
        const newStatus = r.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
        updatedEndpoint = { ...r, status: newStatus };
        return updatedEndpoint;
      }
      return r;
    });
    saveLocalEndpoints(updated);
    if (!updatedEndpoint) throw new Error("Endpoint not found");
    return;
  }
  return apiClient.patch(`/api/v1/endpoints/${id}/toggle`);
}