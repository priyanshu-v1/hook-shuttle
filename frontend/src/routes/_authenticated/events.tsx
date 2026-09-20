import { createFileRoute } from "@tanstack/react-router";
import { Copy, Loader2, RotateCcw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

import { eventTypeOptions, type WebhookEvent } from "@/lib/mock-data";
import { useEvents, useEventAttempts } from "@/hooks/useEvents";

export const Route = createFileRoute("/_authenticated/events")({
  head: () => ({
    meta: [
      { title: "Webhook Events · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Search the hook-shuttle audit trail: filter events by status, type or payload and inspect raw JSON and delivery attempts.",
      },
      { property: "og:title", content: "Webhook Events · hook-shuttle Console" },
      {
        property: "og:description",
        content: "Searchable webhook audit trail with raw payloads and delivery attempt logs.",
      },
    ],
  }),
  component: EventsPage,
});

function time(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "medium" });
}

function EventsPage() {
  const { events, isLoading } = useEvents();
  const [status, setStatus] = useState("ALL");
  const [type, setType] = useState("ALL");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<WebhookEvent | null>(null);

  // Lazily fetch delivery attempts only when an event is selected in the drawer
  const { attempts, isLoadingAttempts } = useEventAttempts(selected?.id ?? null);


  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return events.filter((e) => {
      if (status !== "ALL" && e.status !== status) return false;
      if (type !== "ALL" && e.event_type !== type) return false;
      if (!q) return true;
      return (
        e.id.toLowerCase().includes(q) ||
        e.event_type.toLowerCase().includes(q) ||
        e.target_url.toLowerCase().includes(q) ||
        JSON.stringify(e.payload).toLowerCase().includes(q)
      );
    });
  }, [events, status, type, query]);

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Events & audit trail</h2>
        <p className="text-sm text-muted-foreground">
          Every event received, with full payload and delivery attempt history.
        </p>
      </div>

      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base">{filtered.length} events</CardTitle>
            <CardDescription>Payload search matches any value inside the JSON body</CardDescription>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="absolute top-2.5 left-2.5 size-4 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search id, url or payload…"
                className="w-64 pl-8"
              />
            </div>
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All statuses</SelectItem>
                <SelectItem value="SUCCESS">Success</SelectItem>
                <SelectItem value="FAILED">Failed</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
              </SelectContent>
            </Select>
            <Select value={type} onValueChange={setType}>
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All event types</SelectItem>
                {eventTypeOptions.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {(status !== "ALL" || type !== "ALL" || query) && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStatus("ALL");
                  setType("ALL");
                  setQuery("");
                }}
              >
                <RotateCcw className="size-4" /> Reset
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Event ID</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Target</TableHead>
                <TableHead>Received</TableHead>
                <TableHead className="text-right">Latency</TableHead>
                <TableHead className="text-right">Attempts</TableHead>
                <TableHead className="text-right">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell
                    colSpan={7}
                    className="py-12 text-center text-sm text-muted-foreground"
                  >
                    <Loader2 className="mx-auto size-6 animate-spin" />
                    <span className="mt-2 block">Loading events...</span>
                  </TableCell>
                </TableRow>
              )}
              {!isLoading &&
                filtered.map((e) => (
                  <TableRow
                    key={e.id}
                    onClick={() => setSelected(e)}
                    className="cursor-pointer transition-colors hover:bg-muted/60"
                  >
                    <TableCell className="font-mono text-xs">{e.id}</TableCell>
                    <TableCell className="text-sm">{e.event_type}</TableCell>
                    <TableCell className="max-w-[240px] truncate text-sm text-muted-foreground">
                      {e.target_url}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {time(e.received_at)}
                    </TableCell>
                    <TableCell className="text-right text-sm">
                      {e.latency_ms ? `${e.latency_ms} ms` : "—"}
                    </TableCell>
                    <TableCell className="text-right text-sm">{e.attempts_count ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      <StatusBadge status={e.status} />
                    </TableCell>
                  </TableRow>
                ))}
              {!isLoading && filtered.length === 0 && (
                <TableRow>
                  <TableCell
                    colSpan={7}
                    className="py-12 text-center text-sm text-muted-foreground"
                  >
                    No events match these filters.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Sheet open={selected !== null} onOpenChange={(o) => !o && setSelected(null)}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-xl">
          {selected && (
            <>
              <SheetHeader>
                <SheetTitle className="flex items-center gap-2">
                  <span className="font-mono text-sm">{selected.id}</span>
                  <StatusBadge status={selected.status} />
                </SheetTitle>
                <SheetDescription>
                  {selected.event_type} · {time(selected.received_at)}
                </SheetDescription>
              </SheetHeader>

              <div className="space-y-5 px-4 pb-8">
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">Endpoint</dt>
                    <dd className="font-mono text-xs">{selected.endpoint_id}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Latency</dt>
                    <dd>{selected.latency_ms ? `${selected.latency_ms} ms` : "pending"}</dd>
                  </div>
                  <div className="col-span-2">
                    <dt className="text-xs text-muted-foreground">Target URL</dt>
                    <dd className="break-all text-xs">{selected.target_url}</dd>
                  </div>
                </dl>

                <Separator />

                <Tabs defaultValue="payload">
                  <TabsList className="w-full">
                    <TabsTrigger value="payload" className="flex-1">
                      Payload
                    </TabsTrigger>
                    <TabsTrigger value="attempts" className="flex-1">
                      Delivery attempts ({isLoadingAttempts ? "..." : attempts.length})
                    </TabsTrigger>
                  </TabsList>

                  <TabsContent value="payload" className="mt-3 space-y-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        void navigator.clipboard.writeText(
                          JSON.stringify(selected.payload, null, 2),
                        );
                        toast.success("Payload copied");
                      }}
                    >
                      <Copy className="size-4" /> Copy JSON
                    </Button>
                    <pre className="max-h-[420px] overflow-auto rounded-lg border border-border bg-muted/50 p-3 font-mono text-xs leading-relaxed">
                      {JSON.stringify(selected.payload, null, 2)}
                    </pre>
                  </TabsContent>

                  <TabsContent value="attempts" className="mt-3 space-y-3">
                    {isLoadingAttempts ? (
                      <div className="py-8 text-center text-sm text-muted-foreground">
                        <Loader2 className="mx-auto size-5 animate-spin" />
                        <span className="mt-2 block">Loading delivery attempts...</span>
                      </div>
                    ) : attempts.length === 0 ? (
                      <div className="py-8 text-center text-sm text-muted-foreground">
                        No delivery attempts found for this event.
                      </div>
                    ) : (
                      attempts.map((a) => (
                        <div key={a.attempt} className="rounded-lg border border-border p-3">
                          <div className="flex items-center justify-between">
                            <p className="text-sm font-medium">Attempt #{a.attempt}</p>
                            <span
                              className={
                                a.status_code && a.status_code < 300
                                  ? "font-mono text-xs text-emerald-600 dark:text-emerald-400"
                                  : "font-mono text-xs text-red-600 dark:text-red-400"
                              }
                            >
                              {a.status_code ?? "no response"}
                            </span>
                          </div>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {a.execution_time_ms} ms · {time(a.attempted_at)}
                          </p>
                          {a.error_message && (
                            <p className="mt-2 rounded border border-destructive/25 bg-destructive/10 px-2 py-1 text-xs text-destructive">
                              {a.error_message}
                            </p>
                          )}
                          <details className="mt-2">
                            <summary className="cursor-pointer text-xs text-muted-foreground">
                              Request headers
                            </summary>
                            <pre className="mt-2 overflow-auto rounded bg-muted/50 p-2 font-mono text-[11px]">
                              {JSON.stringify(a.headers, null, 2)}
                            </pre>
                          </details>
                        </div>
                      ))
                    )}
                  </TabsContent>
                </Tabs>
              </div>
            </>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
