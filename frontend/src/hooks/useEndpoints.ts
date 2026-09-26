import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchEndpoints, createEndpoint, toggleEndpointStatus } from "@/api/endpointApi";
import { toast } from "sonner";

export function useEndpoints(page: number, size: number) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["endpoints", page, size],
    queryFn: () => fetchEndpoints(page, size),
  });

  const createMutation = useMutation({
    mutationFn: createEndpoint,
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ["endpoints"] });
      toast.success("Endpoint created", { description: created.id });
    },
    onError: (error: any) => {
      toast.error("Failed to create endpoint", { description: error?.message });
    },
  });

  const toggleMutation = useMutation({
    mutationFn: toggleEndpointStatus,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["endpoints"] });
    },
    onError: (error: any) => {
      toast.error("Failed to update status", { description: error?.message });
    },
  });

  return {
    ...query,
    endpoints: query.data?.content || [],
    totalElements: query.data?.total_elements || 0,
    createEndpoint: createMutation.mutateAsync,
    toggleEndpoint: toggleMutation.mutate,
    isCreating: createMutation.isPending,
  };
}