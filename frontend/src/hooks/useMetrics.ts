import { useQuery } from "@tanstack/react-query";
import { fetchMetricsSummary } from "@/api/metricsApi";

export function useMetrics() {
  const query = useQuery({
    queryKey: ["metrics-summary"],
    queryFn: fetchMetricsSummary,
    refetchInterval: 30000,
  });

  return {
    ...query,
    data: query.data,
  };
}