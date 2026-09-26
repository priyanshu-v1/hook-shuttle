import { createFileRoute } from "@tanstack/react-router";
import { Check, Copy, KeyRound, Plus, Search, TriangleAlert } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { useEndpoints } from "@/hooks/useEndpoints";
import { ListPagination } from "@/components/list-pagination";

export const Route = createFileRoute("/_authenticated/endpoints")({
  head: () => ({
    meta: [
      { title: "Endpoints · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Manage hook-shuttle target endpoints: URLs, signing secrets, rate limits, timeouts and retry policy.",
      },
      { property: "og:title", content: "Endpoints · hook-shuttle Console" },
      {
        property: "og:description",
        content: "Create and configure webhook target endpoints with retries and rate limits.",
      },
    ],
  }),
  component: EndpointsPage,
});

function EndpointsPage() {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const { endpoints, createEndpoint, toggleEndpoint, isCreating, totalElements } = useEndpoints(page - 1, pageSize);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [open, setOpen] = useState(false);
  
  // States for one-time signing secret reveal modal
  const [newSecret, setNewSecret] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const [form, setForm] = useState({
    target_url: "",
    description: "",
    rate_limit_per_sec: "60",
    timeout_ms: "5000",
    max_retries: "3",
    active: true,
  });

  const filtered = useMemo(
    () =>
      endpoints.filter(
        (r) =>
          (statusFilter === "ALL" || r.status === statusFilter) &&
          (r.target_url.toLowerCase().includes(query.toLowerCase()) ||
            r.description.toLowerCase().includes(query.toLowerCase()) ||
            r.id.includes(query)),
      ),
    [endpoints, query, statusFilter],
  );

  async function handleCreate() {
    if (!form.target_url.startsWith("http")) {
      toast.error("Target URL must start with http:// or https://");
      return;
    }
    const result = await createEndpoint({
      target_url: form.target_url,
      description: form.description || "No description",
      rate_limit_per_sec: Number(form.rate_limit_per_sec) || 60,
      timeout_ms: Number(form.timeout_ms) || 5000,
      max_retries: Number(form.max_retries) || 3,
      active: form.active,
    });
    
    setOpen(false);
    setForm({
      target_url: "",
      description: "",
      rate_limit_per_sec: "60",
      timeout_ms: "5000",
      max_retries: "3",
      active: true,
    });
    setCopied(false);

    // Capture the secret returned from creation response for one-time reveal
    if (result && typeof result === "object" && "secret_key" in result && result.secret_key) {
      setNewSecret(String(result.secret_key));
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Target endpoints</h2>
          <p className="text-sm text-muted-foreground">
            Destinations hook-shuttle delivers verified webhooks to.
          </p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="size-4" /> Create endpoint
        </Button>
      </div>

      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base">{filtered.length} endpoints</CardTitle>
            <CardDescription>Search by URL, description or id</CardDescription>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="absolute top-2.5 left-2.5 size-4 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search endpoints…"
                className="w-56 pl-8"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All statuses</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="DISABLED">Disabled</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent className="overflow-x-auto px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Target URL</TableHead>
                <TableHead>Description</TableHead>
                <TableHead className="text-right">Rate/s</TableHead>
                <TableHead className="text-right">Timeout</TableHead>
                <TableHead className="text-right">Retries</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Enabled</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((r) => (
                <TableRow key={r.id} className="transition-colors">
                  <TableCell className="font-mono text-xs">{r.id}</TableCell>
                  <TableCell className="max-w-[260px] truncate text-sm">{r.target_url}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{r.description}</TableCell>
                  <TableCell className="text-right text-sm">{r.rate_limit_per_sec}</TableCell>
                  <TableCell className="text-right text-sm">{r.timeout_ms} ms</TableCell>
                  <TableCell className="text-right text-sm">{r.max_retries}</TableCell>
                  <TableCell>
                    <StatusBadge status={r.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    <Switch
                      checked={r.status === "ACTIVE"}
                      onCheckedChange={() => toggleEndpoint(r.id)}
                      aria-label="Toggle endpoint"
                    />
                  </TableCell>
                </TableRow>
              ))}
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

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Create endpoint</DialogTitle>
            <DialogDescription>
              A signing secret is generated automatically and used to sign every delivery.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="url">Target URL</Label>
              <Input
                id="url"
                value={form.target_url}
                onChange={(e) => setForm({ ...form, target_url: e.target.value })}
                placeholder="https://api.example.com/hooks"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="desc">Description</Label>
              <Textarea
                id="desc"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="What this endpoint receives"
                rows={2}
              />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label htmlFor="rate">Rate limit /s</Label>
                <Input
                  id="rate"
                  type="number"
                  value={form.rate_limit_per_sec}
                  onChange={(e) => setForm({ ...form, rate_limit_per_sec: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="timeout">Timeout (ms)</Label>
                <Input
                  id="timeout"
                  type="number"
                  value={form.timeout_ms}
                  onChange={(e) => setForm({ ...form, timeout_ms: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="retries">Max retries</Label>
                <Input
                  id="retries"
                  type="number"
                  value={form.max_retries}
                  onChange={(e) => setForm({ ...form, max_retries: e.target.value })}
                />
              </div>
            </div>
            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <p className="text-sm font-medium">Activate immediately</p>
                <p className="text-xs text-muted-foreground">
                  Disabled endpoints queue nothing and reject deliveries.
                </p>
              </div>
              <Switch
                checked={form.active}
                onCheckedChange={(v) => setForm({ ...form, active: v })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate}>
              <KeyRound className="size-4" /> {isCreating ? "Creating..." : "Create endpoint"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* One-time signing secret reveal dialog */}
      <Dialog
        open={newSecret !== null}
        onOpenChange={(o) => {
          if (!o) {
            setNewSecret(null);
            setCopied(false);
          }
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Copy your signing secret</DialogTitle>
            <DialogDescription>
              This is the only time the full signing secret will be visible.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-300">
              <TriangleAlert className="mt-0.5 size-4 shrink-0" />
              <p>Once you close this dialog the signing secret cannot be retrieved again.</p>
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/50 p-3">
              <code className="min-w-0 flex-1 truncate font-mono text-xs">{newSecret}</code>
              <Button
                size="sm"
                variant={copied ? "secondary" : "default"}
                onClick={() => {
                  void navigator.clipboard.writeText(newSecret ?? "");
                  setCopied(true);
                  toast.success("Signing secret copied");
                }}
              >
                {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
                {copied ? "Copied" : "Copy"}
              </Button>
            </div>
          </div>
          <DialogFooter>
            <Button
              onClick={() => {
                setNewSecret(null);
                setCopied(false);
              }}
            >
              I've stored it safely
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}