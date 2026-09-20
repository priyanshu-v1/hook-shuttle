import { useQuery } from "@tanstack/react-query";
import { fetchEvents, fetchEventAttempts } from "@/api/eventApi";
import { toast } from "sonner";

export function useEvents() {
  const { data: events = [], isLoading, error } = useQuery({
    queryKey: ["webhook-events"],
    queryFn: fetchEvents,
  });

  return {
    events,
    isLoading,
    error,
  };
}

export function useEventAttempts(eventId: string | null) {
  const { data: attempts = [], isLoading: isLoadingAttempts } = useQuery({
    queryKey: ["event-attempts", eventId],
    queryFn: () => fetchEventAttempts(eventId!),
    enabled: !!eventId, // Only fetches when a specific event ID is selected!
  });

  return {
    attempts,
    isLoadingAttempts,
  };
}