import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Loader2, Lock, Rocket } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getSession } from "@/lib/session";
import { useAuth } from "@/hooks/useAuth"; // Import your new auth hook

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Sign in · hook-shuttle Console" },
      {
        name: "description",
        content:
          "Secure operator sign-in for the hook-shuttle console: monitor webhook delivery, endpoints, API keys and audit trails.",
      },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("operator@hook-shuttle.io");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Use the auth hook
  const { login, isLoggingIn } = useAuth();

  useEffect(() => {
    if (getSession()) navigate({ to: "/dashboard", replace: true });
  }, [navigate]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email.includes("@") || password.length < 6) {
      setError("Enter a valid email and a password of at least 6 characters.");
      return;
    }

    try {
      await login({ email, password });
      // Navigation and success toast are handled inside the mutation's onSuccess
    } catch (err: any) {
      setError(err?.response?.data?.message || "Authentication failed. Check your credentials.");
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4">
      <div className="pointer-events-none absolute inset-0 [background:radial-gradient(60%_50%_at_50%_0%,color-mix(in_oklab,var(--primary)_14%,transparent),transparent)]" />
      <div className="pointer-events-none absolute inset-0 opacity-[0.06] [background-image:linear-gradient(to_right,var(--foreground)_1px,transparent_1px),linear-gradient(to_bottom,var(--foreground)_1px,transparent_1px)] [background-size:44px_44px]" />

      <div className="relative w-full max-w-sm animate-in fade-in slide-in-from-bottom-2 duration-500">
        <div className="mb-6 flex flex-col items-center gap-3 text-center">
          <div className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <Rocket className="size-5" />
          </div>
          <div>
            <h1 className="text-xl font-semibold tracking-tight">hook-shuttle console</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Sign in to manage your webhook gateway
            </p>
          </div>
        </div>

        <form
          onSubmit={submit}
          className="space-y-4 rounded-xl border border-border bg-card p-6 shadow-sm"
        >
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <Button type="submit" className="w-full" disabled={isLoggingIn}>
            {isLoggingIn ? <Loader2 className="size-4 animate-spin" /> : <Lock className="size-4" />}
            {isLoggingIn ? "Verifying…" : "Sign in"}
          </Button>

          <p className="text-center text-xs text-muted-foreground">
            Access is provisioned by your workspace administrator.
          </p>
        </form>
      </div>
    </div>
  );
}