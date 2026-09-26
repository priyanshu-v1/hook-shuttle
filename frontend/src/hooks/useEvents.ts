import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchEvents, fetchEventAttempts, replayEvent } from "@/api/eventApi";
import { toast } from "sonner";

export function useEvents(page: number, size: number, status?: string) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["webhook-events", page, size, status],
    queryFn: () => fetchEvents(page, size, status),
  });

  const replayMutation = useMutation({
    mutationFn: (eventId: string) => replayEvent(eventId),
    onSuccess: (_, eventId) => {
      toast.success("Replay triggered", {
        description: `Event ${eventId} has been successfully re-queued for delivery.`,
      });
      void queryClient.invalidateQueries({ queryKey: ["webhook-events"] });
      void queryClient.invalidateQueries({ queryKey: ["event-attempts", eventId] });
    },
    onError: (error) => {
      toast.error("Replay failed", {
        description: error.message || "Could not queue event for redelivery.",
      });
    },
  });

  return {
    ...query,
    events: query.data?.content || [],
    totalElements: query.data?.total_elements || 0,
    replayEvent: replayMutation.mutate,
    isReplaying: replayMutation.isPending,
  };
}

export function useEventAttempts(eventId: string | null) {
  const { data: attempts = [], isLoading: isLoadingAttempts } = useQuery({
    queryKey: ["event-attempts", eventId],
    queryFn: () => fetchEventAttempts(eventId!),
    enabled: !!eventId,
  });

  return {
    attempts,
    isLoadingAttempts,
  };
}