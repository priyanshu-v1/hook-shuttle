import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { createApiKey, fetchApiKeys, revokeApiKey } from "@/api/apiKeyApi";

export function useApiKeys() {
  const queryClient = useQueryClient();

  const { data: apiKeys = [], isLoading } = useQuery({
    queryKey: ["api-keys"],
    queryFn: fetchApiKeys,
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
    apiKeys,
    isLoading,
    createApiKey: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    revokeApiKey: revokeMutation.mutate,
    isRevoking: revokeMutation.isPending,
  };
}