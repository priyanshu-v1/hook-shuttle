import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { Activity, KeyRound, LayoutDashboard, LogOut, Radio, Rocket } from "lucide-react";

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { getSession, signOut } from "@/lib/session";

const items = [
  { title: "Overview", url: "/dashboard", icon: LayoutDashboard },
  { title: "Endpoints", url: "/endpoints", icon: Radio },
  { title: "API Keys", url: "/api-keys", icon: KeyRound },
  { title: "Events", url: "/events", icon: Activity },
];

export function AppSidebar() {
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const email = getSession()?.email ?? "operator@hook-shuttle.io";

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="border-b border-sidebar-border">
        <div className="flex items-center gap-2 px-1 py-1.5">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Rocket className="size-4" />
          </div>
          <div className="grid min-w-0 leading-tight group-data-[collapsible=icon]:hidden">
            <span className="truncate text-sm font-semibold">hook-shuttle</span>
            <span className="truncate text-xs text-muted-foreground">webhook gateway</span>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Platform</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {items.map((item) => (
                <SidebarMenuItem key={item.url}>
                  <SidebarMenuButton asChild isActive={pathname === item.url} tooltip={item.title}>
                    <Link to={item.url} className="flex items-center gap-2">
                      <item.icon className="size-4" />
                      <span>{item.title}</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              tooltip="Sign out"
              onClick={() => {
                signOut();
                navigate({ to: "/", replace: true });
              }}
            >
              <LogOut className="size-4" />
              <span className="truncate">{email}</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
