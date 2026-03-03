import { cn } from "@/lib/utils";

type StatusType = "online" | "offline" | "away" | "busy" | "pending" | "success" | "warning" | "error";

const statusColors: Record<StatusType, string> = {
  online: "bg-green-500",
  offline: "bg-slate-400",
  away: "bg-amber-500",
  busy: "bg-red-500",
  pending: "bg-blue-500",
  success: "bg-green-500",
  warning: "bg-amber-500",
  error: "bg-red-500",
};

interface StatusDotProps {
  status: StatusType;
  pulse?: boolean;
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function StatusDot(
  { status, pulse = false, size = "md", className }: Readonly<StatusDotProps>
) {
  const sizeClasses = {
    sm: "h-2 w-2",
    md: "h-2.5 w-2.5",
    lg: "h-3 w-3",
  };

  return (
    <span className={cn("relative flex", className)}>
      <span
        className={cn(
          "inline-block rounded-full",
          sizeClasses[size],
          statusColors[status],
          pulse && "animate-pulse"
        )}
      />
      {pulse && (
        <span
          className={cn(
            "absolute inline-flex h-full w-full rounded-full opacity-75",
            statusColors[status],
            "animate-ping"
          )}
        />
      )}
    </span>
  );
}
