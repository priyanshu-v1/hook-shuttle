import { apiClient } from "./client";
import { metrics as seedMetrics, throughput as seedThroughput, statusBreakdown as seedStatusBreakdown } from "@/lib/mock-data";

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";

export interface MetricsSummaryResponse {
  metrics: {
    total_events: number;
    success_rate: number;
    p50_latency: number;
    active_endpoints: number;
    events_delta: string;
    success_delta: string;
    latency_delta: string;
    endpoints_delta: string;
  };
  throughput: Array<{
    time: string;
    delivered: number;
    failed: number;
  }>;
  status_breakdown: Array<{
    name: string;
    value: number;
    key: string;
  }>;
}

export async function fetchMetricsSummary(): Promise<MetricsSummaryResponse> {
  if (USE_MOCK) {
    return {
      metrics: {
        total_events: seedMetrics.totalEvents,
        success_rate: seedMetrics.successRate,
        p50_latency: seedMetrics.p50Latency,
        active_endpoints: seedMetrics.activeEndpoints,
        events_delta: seedMetrics.eventsDelta,
        success_delta: seedMetrics.successDelta,
        latency_delta: seedMetrics.latencyDelta,
        endpoints_delta: seedMetrics.endpointsDelta,
      },
      throughput: seedThroughput,
      status_breakdown: seedStatusBreakdown,
    };
  }
  return apiClient.get("/api/v1/metrics/summary");
}