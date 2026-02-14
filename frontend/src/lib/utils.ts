import { type ClassValue, clsx } from "clsx";
import { format, formatDistanceToNow, isToday, isYesterday, parseISO } from "date-fns";
import { startCase } from "lodash-es";
import { twMerge } from "tailwind-merge";

type DateInput = string | Date | null | undefined;

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function toDateValue(value: Exclude<DateInput, null | undefined>): Date {
  if (typeof value === "string") {
    return parseISO(value);
  }

  return value;
}

export function formatDateTime(date: DateInput): string {
  if (!date) return "-";
  const d = toDateValue(date);
  if (isToday(d)) return `Today, ${format(d, "h:mm a")}`;
  if (isYesterday(d)) return `Yesterday, ${format(d, "h:mm a")}`;
  return format(d, "MMM d, yyyy, h:mm a");
}

export function formatDate(date: DateInput): string {
  if (!date) return "-";
  const d = toDateValue(date);
  return format(d, "MMM d, yyyy");
}

export function formatRelativeTime(date: DateInput): string {
  if (!date) return "-";
  const d = toDateValue(date);
  return formatDistanceToNow(d, { addSuffix: true });
}

type StatusTone = "default" | "success" | "warning" | "destructive" | "secondary";

export function getStatusTone(status: string): StatusTone {
  const s = status.toUpperCase();
  if (["RESOLVED", "CLOSED"].includes(s)) return "success";
  if (["ESCALATED", "AUTO_CLOSED_PENDING"].includes(s)) return "destructive";
  if (["TRIAGING", "WAITING_CUSTOMER"].includes(s)) return "warning";
  if (["RECEIVED"].includes(s)) return "secondary";
  return "default";
}

export function getPriorityTone(priority: string | null | undefined): StatusTone {
  if (!priority) return "secondary";
  const p = priority.toUpperCase();
  if (p === "CRITICAL") return "destructive";
  if (p === "HIGH") return "warning";
  if (p === "MEDIUM") return "default";
  return "secondary";
}

export function formatLabel(raw: string | null | undefined): string {
  if (!raw) {
    return "-";
  }

  const formatted = startCase(raw.toLowerCase()).trim();
  if (!formatted) {
    return "-";
  }

  return formatted;
}
