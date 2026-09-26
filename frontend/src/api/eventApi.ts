import { apiClient } from "./client";
import {
  webhookEvents as seedEvents,
  type WebhookEvent,
  type DeliveryAttempt,
  SpringPage,
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

export async function fetchEvents(
  page: number,
  size: number,
  status?: string
): Promise<SpringPage<WebhookEvent>> {
  if (USE_MOCK) {
    const raw = getLocalRawEvents();
    const filteredRaw = status && status !== "ALL" 
      ? raw.filter((e) => e.status === status)
      : raw;
    
    const mapped = filteredRaw.map(({ attempts, ...event }) => ({ 
      ...event, 
      attempts_count: attempts.length 
    }));
    
    const start = page * size;
    const content = mapped.slice(start, start + size);

    return {
      content,
      total_elements: mapped.length,
      total_pages: Math.ceil(mapped.length / size),
      number: page,
      size,
    };
  }
  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    ...(status && status !== "ALL" ? { status } : {}),
  });
  return apiClient.get(`/api/v1/events?${queryParams.toString()}`);
}

export async function fetchEventAttempts(eventId: string): Promise<DeliveryAttempt[]> {
  if (USE_MOCK) {
    const raw = getLocalRawEvents();
    const event = raw.find((e) => e.id === eventId);
    return event ? event.attempts : [];
  }
  return apiClient.get(`/api/v1/events/${eventId}/attempts`);
}

export async function replayEvent(eventId: string): Promise<void> {
  if (USE_MOCK) {
    const raw = getLocalRawEvents();
    const event = raw.find((e) => e.id === eventId);
    if (event) {
      const newAttempt: DeliveryAttempt = {
        attempt: event.attempts.length + 1,
        status_code: 200,
        execution_time_ms: Math.floor(Math.random() * 150) + 50,
        attempted_at: new Date().toISOString(),
        error_message: null,
        headers: { "X-Webhook-Event": event.event_type, "X-Manual-Replay": "true" },
        trigger_type: "MANUAL_REPLAY"
      };
      event.attempts.push(newAttempt);
      event.status = "SUCCESS";
      saveLocalRawEvents(raw);
    }
    return;
  }
  return apiClient.post(`/api/v1/events/${eventId}/replay`, {});
}