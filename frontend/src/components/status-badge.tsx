import { cn } from "@/lib/utils";

const tones: Record<string, string> = {
  SUCCESS: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/25",
  ACTIVE: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/25",
  FAILED: "bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/25",
  REVOKED: "bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/25",
  PENDING: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/25",
  DISABLED: "bg-muted text-muted-foreground border-border",
};

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-xs font-medium tracking-tight",
        tones[status] ?? tones["DISABLED"],
        className,
      )}
    >
      <span className="size-1.5 rounded-full bg-current" />
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  );
}
