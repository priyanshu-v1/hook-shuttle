import { createFileRoute, Link } from "@tanstack/react-router";
import { Activity, CheckCircle2, Radio, Timer } from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { StatusBadge } from "@/components/status-badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useEvents } from "@/hooks/useEvents";
import { useMetrics } from "@/hooks/useMetrics";

export const Route = createFileRoute("/_authenticated/dashboard")({
  head: () => ({
    meta: [
      { title: "Overview · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Live delivery metrics for hook-shuttle: events processed, success rate, latency and endpoint health.",
      },
      { property: "og:title", content: "Overview · hook-shuttle Console" },
      {
        property: "og:description",
        content: "Live webhook delivery metrics, throughput trends and status breakdown.",
      },
    ],
  }),
  component: DashboardPage,
});

const barColors = ["var(--color-chart-2)", "var(--color-destructive)", "var(--color-chart-4)"];

function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 text-xs shadow-md">
      <p className="mb-1 font-medium text-popover-foreground">{label}</p>
      {payload.map((p: any) => (
        <p key={p.name} className="text-muted-foreground">
          <span className="font-medium text-popover-foreground">{p.name}</span>:{" "}
          {Number(p.value).toLocaleString()}
        </p>
      ))}
    </div>
  );
}

function DashboardPage() {
  const { data: metricsData, isLoading: isMetricsLoading } = useMetrics();
  const { events, isLoading: isEventsLoading } = useEvents(0, 6);
  const recent = events.slice(0, 6);

  // Fallback structures if loading or empty
  const rawMetrics = metricsData?.metrics;
  const cards = [
    {
      label: "Events processed",
      value: rawMetrics ? rawMetrics.total_events.toLocaleString() : "—",
      delta: rawMetrics?.events_delta || "—",
      icon: Activity,
      hint: "last 30 days",
    },
    {
      label: "Success rate",
      value: rawMetrics ? `${rawMetrics.success_rate}%` : "—",
      delta: rawMetrics?.success_delta || "—",
      icon: CheckCircle2,
      hint: "rolling 24h",
    },
    {
      label: "P50 Latency",
      value: rawMetrics ? `${rawMetrics.p50_latency} ms` : "—",
      delta: rawMetrics?.latency_delta || "—",
      icon: Timer,
      hint: "p50 delivery",
    },
    {
      label: "Active endpoints",
      value: rawMetrics ? String(rawMetrics.active_endpoints) : "—",
      delta: rawMetrics?.endpoints_delta || "—",
      icon: Radio,
      hint: "receiving traffic",
    },
  ];

  const throughput = metricsData?.throughput || [];
  const statusBreakdown = metricsData?.status_breakdown || [];

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((c) => (
          <Card key={c.label} className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between gap-2 pb-2">
              <CardDescription>{c.label}</CardDescription>
              <c.icon className="size-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold tracking-tight">
                {isMetricsLoading ? "..." : c.value}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                <span className="font-medium text-emerald-600 dark:text-emerald-400">
                  {c.delta}
                </span>{" "}
                · {c.hint}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Delivery throughput</CardTitle>
            <CardDescription>Deliveries vs failures over the last 24 hours</CardDescription>
          </CardHeader>
          <CardContent className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={throughput} margin={{ left: -12, right: 8, top: 4 }}>
                <defs>
                  <linearGradient id="ok" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--color-chart-2)" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="var(--color-chart-2)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="var(--color-border)"
                  vertical={false}
                />
                <XAxis
                  dataKey="time"
                  stroke="var(--color-muted-foreground)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="var(--color-muted-foreground)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <Tooltip content={<ChartTooltip />} />
                <Area
                  type="monotone"
                  dataKey="delivered"
                  name="Delivered"
                  stroke="var(--color-chart-2)"
                  strokeWidth={2}
                  fill="url(#ok)"
                />
                <Area
                  type="monotone"
                  dataKey="failed"
                  name="Failed"
                  stroke="var(--color-destructive)"
                  strokeWidth={2}
                  fill="none"
                />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Status breakdown</CardTitle>
            <CardDescription>Last 24 hours by delivery outcome</CardDescription>
          </CardHeader>
          <CardContent className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusBreakdown} margin={{ left: -12, right: 8, top: 4 }}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="var(--color-border)"
                  vertical={false}
                />
                <XAxis
                  dataKey="name"
                  stroke="var(--color-muted-foreground)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="var(--color-muted-foreground)"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ fill: "var(--color-muted)" }} />
                <Bar dataKey="value" name="Events" radius={[6, 6, 0, 0]}>
                  {statusBreakdown.map((entry, i) => (
                    <Cell key={entry.key} fill={barColors[i]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="text-base">Recent deliveries</CardTitle>
            <CardDescription>Latest events routed through the gateway</CardDescription>
          </div>
          <Link to="/events" className="text-sm font-medium text-primary hover:underline">
            View all
          </Link>
        </CardHeader>
        <CardContent className="px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Event</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Target</TableHead>
                <TableHead>Latency</TableHead>
                <TableHead className="text-right">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isEventsLoading ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center text-sm text-muted-foreground">
                    Loading recent webhook events...
                  </TableCell>
                </TableRow>
              ) : recent.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-24 text-center text-sm text-muted-foreground">
                    No webhook events found.
                  </TableCell>
                </TableRow>
              ) : (
                recent.map((e) => (
                  <TableRow key={e.id}>
                    <TableCell className="font-mono text-xs">{e.id}</TableCell>
                    <TableCell className="text-sm">{e.event_type}</TableCell>
                    <TableCell className="max-w-[280px] truncate text-sm text-muted-foreground">
                      {e.target_url}
                    </TableCell>
                    <TableCell className="text-sm">
                      {e.latency_ms ? `${e.latency_ms} ms` : "—"}
                    </TableCell>
                    <TableCell className="text-right">
                      <StatusBadge status={e.status} />
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
