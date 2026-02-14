import { type ClassValue, clsx } from "clsx";
import { format, formatDistanceToNow, isToday, isYesterday, parseISO } from "date-fns";
import { startCase } from "lodash-es";
import { twMerge } from "tailwind-merge";
import { getTicketPriorityTone, getTicketStatusTone, type StatusTone } from "@/lib/ticket-visuals";

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

export function getStatusTone(status: string): StatusTone {
  return getTicketStatusTone(status);
}

export function getPriorityTone(priority: string | null | undefined): StatusTone {
  return getTicketPriorityTone(priority);
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
