import { createFileRoute, Outlet, redirect, useRouterState } from "@tanstack/react-router";
import { Moon, Sun } from "lucide-react";

import { AppSidebar } from "@/components/app-sidebar";
import { Button } from "@/components/ui/button";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { getSession } from "@/lib/session";
import { useTheme } from "@/lib/theme";

export const Route = createFileRoute("/_authenticated")({
  ssr: false,
  beforeLoad: () => {
    if (!getSession()) throw redirect({ to: "/" });
  },
  component: DashboardLayout,
});

const titles: Record<string, string> = {
  "/dashboard": "Overview",
  "/endpoints": "Endpoints",
  "/api-keys": "API Keys",
  "/events": "Webhook Events",
  "/dlq": "Dead Letter Queue",
};

function DashboardLayout() {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const { theme, toggle } = useTheme();

  return (
    <SidebarProvider>
      <div className="flex min-h-screen w-full bg-background">
        <AppSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur">
            <SidebarTrigger />
            <Separator orientation="vertical" className="h-5" />
            <h1 className="text-sm font-medium">{titles[pathname] ?? "hook-shuttle"}</h1>
            <div className="ml-auto flex items-center gap-2">
              <span className="hidden items-center gap-1.5 rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-600 sm:inline-flex dark:text-emerald-400">
                <span className="size-1.5 animate-pulse rounded-full bg-current" />
                All systems operational
              </span>
              <Button variant="ghost" size="icon" onClick={toggle} aria-label="Toggle theme">
                {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
              </Button>
            </div>
          </header>
          <main className="flex-1 animate-in fade-in duration-300 px-4 py-6 sm:px-6 lg:px-8">
            <Outlet />
          </main>
        </div>
      </div>
    </SidebarProvider>
  );
}
