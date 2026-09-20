import { apiClient } from "./client";
import {
  webhookEvents as seedEvents,
  type WebhookEvent,
  type DeliveryAttempt,
} from "@/lib/mock-data";

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";
const STORAGE_KEY = "hook_shuttle_events";

// Raw stored events contain the nested attempts from seed data, allowing us to split them cleanly
interface RawStoredEvent extends WebhookEvent {
  attempts: DeliveryAttempt[];
}

function getLocalRawEvents(): RawStoredEvent[] {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seedEvents));
    return seedEvents as RawStoredEvent[];
  }
  return JSON.parse(stored);
}

function saveLocalRawEvents(events: RawStoredEvent[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
}

export async function fetchEvents(): Promise<WebhookEvent[]> {
  if (USE_MOCK) {
    const raw = getLocalRawEvents();
    // Return events without the heavy attempts array to mirror production list endpoint
    return raw.map(({ attempts, ...event }) => ({ ...event, attempts_count: attempts.length }));
  }
  return apiClient.get("/api/v1/events");
}

export async function fetchEventAttempts(eventId: string): Promise<DeliveryAttempt[]> {
  if (USE_MOCK) {
    const raw = getLocalRawEvents();
    const event = raw.find((e) => e.id === eventId);
    return event ? event.attempts : [];
  }
  return apiClient.get(`/api/v1/events/${eventId}/attempts`);
}
