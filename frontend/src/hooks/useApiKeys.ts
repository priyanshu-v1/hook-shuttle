import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { createApiKey, fetchApiKeys, revokeApiKey } from "@/api/apiKeyApi";

export function useApiKeys(page: number, size: number) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["api-keys", page, size],
    queryFn: () => fetchApiKeys(page, size),
  });

  const createMutation = useMutation({
    mutationFn: createApiKey,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys"] });
      toast.success("API key generated successfully");
      return data;
    },
    onError: () => {
      toast.error("Failed to generate API key");
    },
  });

  const revokeMutation = useMutation({
    mutationFn: revokeApiKey,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["api-keys"] });
      toast.success("API key revoked");
    },
    onError: () => {
      toast.error("Failed to revoke API key");
    },
  });

  return {
    ...query,
    apiKeys: query.data?.content || [],
    totalElements: query.data?.total_elements || 0,
    createApiKey: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    revokeApiKey: revokeMutation.mutate,
    isRevoking: revokeMutation.isPending,
  };
}