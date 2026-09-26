import { createFileRoute } from "@tanstack/react-router";
import { Check, Copy, KeyRound, Plus, ShieldAlert, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { StatusBadge } from "@/components/status-badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
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
import { Switch } from "@/components/ui/switch";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

import { useApiKeys } from "@/hooks/useApiKeys";
import type { ApiKey } from "@/lib/mock-data";
import { ListPagination } from "@/components/list-pagination";

export const Route = createFileRoute("/_authenticated/api-keys")({
  head: () => ({
    meta: [
      { title: "API Keys · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Generate, audit and revoke hook-shuttle API keys with one-time secret reveal and last-used tracking.",
      },
      { property: "og:title", content: "API Keys · hook-shuttle Console" },
      {
        property: "og:description",
        content: "Generate and revoke gateway API keys with secure one-time reveal.",
      },
    ],
  }),
  component: ApiKeysPage,
});

function fmt(value: string | null) {
  if (!value) return "Never used";
  return new Date(value).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function ApiKeysPage() {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { apiKeys, totalElements, createApiKey, revokeApiKey, isCreating } = useApiKeys(page - 1, pageSize);
  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState("");
  const [live, setLive] = useState(true);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState<ApiKey | null>(null);

  async function handleGenerate() {
    if (name.trim().length < 3) {
      toast.error("Give the key a recognisable name (3+ characters).");
      return;
    }
    const result = await createApiKey({ key_name: name.trim(), live });
    setCreateOpen(false);
    setName("");
    setCopied(false);
    if (result && "api_key" in result && result.api_key) {
      setNewKey(result.api_key);
    }
  }

  function handleRevoke() {
    if (!revokeTarget) return;
    revokeApiKey(revokeTarget.id);
    setRevokeTarget(null);
  }

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">API keys</h2>
          <p className="text-sm text-muted-foreground">
            Keys authenticate producers publishing events to the gateway.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" /> Generate new key
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{apiKeys.length} keys</CardTitle>
          <CardDescription>Secrets are shown once at creation and never stored in full.</CardDescription>
        </CardHeader>
        <CardContent className="overflow-x-auto px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Prefix</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Last used</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {apiKeys.map((k) => (
                <TableRow key={k.id}>
                  <TableCell className="text-sm font-medium">{k.key_name}</TableCell>
                  <TableCell>
                    <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs">
                      {k.key_prefix}…
                    </code>
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={k.status} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{fmt(k.last_used_at)}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{fmt(k.created_at)}</TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive hover:text-destructive"
                      disabled={k.status === "REVOKED"}
                      onClick={() => setRevokeTarget(k)}
                    >
                      <ShieldAlert className="size-4" /> Revoke
                    </Button>
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
              setPage(1); // Reset to page 1 on size change
            }}
          />
        </CardContent>
      </Card>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Generate API key</DialogTitle>
            <DialogDescription>
              The full secret is displayed once — store it in your secret manager.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="keyname">Key name</Label>
              <Input
                id="keyname"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Checkout service"
              />
            </div>
            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <p className="text-sm font-medium">Live mode key</p>
                <p className="text-xs text-muted-foreground">
                  {live ? "Prefix hs_live_" : "Prefix hs_test_"}
                </p>
              </div>
              <Switch checked={live} onCheckedChange={setLive} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleGenerate}>
              <KeyRound className="size-4" /> {isCreating ? "Generating..." : "Generate"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={newKey !== null}
        onOpenChange={(o) => {
          if (!o) {
            setNewKey(null);
            setCopied(false);
          }
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Copy your new API key</DialogTitle>
            <DialogDescription>
              This is the only time the full key will be visible.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-300">
              <TriangleAlert className="mt-0.5 size-4 shrink-0" />
              <p>Once you close this dialog the secret cannot be retrieved again.</p>
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/50 p-3">
              <code className="min-w-0 flex-1 truncate font-mono text-xs">{newKey}</code>
              <Button
                size="sm"
                variant={copied ? "secondary" : "default"}
                onClick={() => {
                  void navigator.clipboard.writeText(newKey ?? "");
                  setCopied(true);
                  toast.success("API key copied");
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
                setNewKey(null);
                setCopied(false);
              }}
            >
              I've stored it safely
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={revokeTarget !== null} onOpenChange={(o) => !o && setRevokeTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Revoke “{revokeTarget?.key_name}”?</AlertDialogTitle>
            <AlertDialogDescription>
              Requests signed with this key will start failing immediately. This cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleRevoke}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Revoke key
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
