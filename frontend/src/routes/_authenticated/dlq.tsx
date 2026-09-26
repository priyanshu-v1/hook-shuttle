import { createFileRoute } from "@tanstack/react-router";
import { Copy, History, Loader2, RotateCcw, Search } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { ListPagination } from "@/components/list-pagination";
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
import { eventTypeOptions, webhookEvents, type WebhookEvent } from "@/lib/mock-data";
import { useEventAttempts, useEvents } from "@/hooks/useEvents";

export const Route = createFileRoute("/_authenticated/dlq")({
  head: () => ({
    meta: [
      { title: "Dead Letter Queue · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Inspect failed webhook deliveries in the hook-shuttle dead letter queue and replay them manually with one click.",
      },
      { property: "og:title", content: "Dead Letter Queue · hook-shuttle Console" },
      {
        property: "og:description",
        content:
          "Failed webhook deliveries with raw payloads, full attempt logs and one-click manual replay.",
      },
    ],
  }),
  component: DlqPage,
});

function time(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "medium" });
}

function DlqPage() {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { events, totalElements, isLoading, replayEvent, isReplaying } = useEvents(page - 1, pageSize, "FAILED");
  const [type, setType] = useState("ALL");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<WebhookEvent | null>(null);

  const { attempts, isLoadingAttempts } = useEventAttempts(selected?.id ?? null);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return events.filter((e) => {
      if (type !== "ALL" && e.event_type !== type) return false;
      if (!q) return true;
      return (
        e.id.toLowerCase().includes(q) ||
        e.event_type.toLowerCase().includes(q) ||
        e.target_url.toLowerCase().includes(q) ||
        JSON.stringify(e.payload).toLowerCase().includes(q)
      );
    });
  }, [events, type, query]);

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Dead letter queue</h2>
        <p className="text-sm text-muted-foreground">
          Deliveries that exhausted all retries. Inspect the failure and replay manually.
        </p>
      </div>

      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base">{filtered.length} failed events</CardTitle>
            <CardDescription>Payload search matches any value inside the JSON body</CardDescription>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="absolute top-2.5 left-2.5 size-4 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setPage(1);
                }}
                placeholder="Search id, url or payload…"
                className="w-64 pl-8"
              />
            </div>
            <Select
              value={type}
              onValueChange={(value) => {
                setType(value);
                setPage(1);
              }}
            >
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
            {(type !== "ALL" || query) && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setType("ALL");
                  setQuery("");
                  setPage(1);
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
                <TableHead>Target URL</TableHead>
                <TableHead>Failed at</TableHead>
                <TableHead className="text-right">Attempts</TableHead>
                <TableHead className="text-right">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell
                    colSpan={6}
                    className="py-12 text-center text-sm text-muted-foreground"
                  >
                    <Loader2 className="mx-auto size-6 animate-spin" />
                    <span className="mt-2 block">Loading DLQ events...</span>
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
                    <TableCell className="max-w-[260px] truncate text-sm text-muted-foreground">
                      {e.target_url}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {time(e.received_at)}
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
                    colSpan={6}
                    className="py-12 text-center text-sm text-muted-foreground"
                  >
                    The dead letter queue is empty — no failed events match these filters.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
          <ListPagination
            page={page}
            pageSize={pageSize}
            totalItems={totalElements}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(1);
            }}
          />
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
                  {selected.event_type} · failed {time(selected.received_at)}
                </SheetDescription>
              </SheetHeader>

              <div className="space-y-5 px-4 pb-8">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-xs text-muted-foreground">
                    Manual replay redelivers the stored payload to the target endpoint.
                  </p>
                  <Button size="sm" disabled={isReplaying} onClick={() => replayEvent(selected.id)}>
                    {isReplaying ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : (
                      <RotateCcw className="size-4" />
                    )}
                    Replay Event
                  </Button>
                </div>

                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">Endpoint</dt>
                    <dd className="font-mono text-xs">{selected.endpoint_id}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Failed attempts</dt>
                    <dd>{attempts.length}</dd>
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
                            <div className="flex items-center gap-2">
                              <p className="text-sm font-medium">Attempt #{a.attempt}</p>
                              <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] uppercase text-muted-foreground">
                                {a.trigger_type}
                              </span>
                            </div>
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
