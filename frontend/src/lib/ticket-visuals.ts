export type StatusTone = "default" | "success" | "warning" | "destructive" | "secondary";

type StatusVisual = {
  tone: StatusTone;
  dotClass: string;
  iconClass: string;
};

type PriorityVisual = {
  tone: StatusTone;
  borderClass: string;
};

const STATUS_VISUALS: Record<string, StatusVisual> = {
  RECEIVED: { tone: "default", dotClass: "bg-blue-400", iconClass: "text-blue-600" },
  TRIAGING: { tone: "warning", dotClass: "bg-blue-500", iconClass: "text-blue-600" },
  WAITING_CUSTOMER: { tone: "warning", dotClass: "bg-amber-500", iconClass: "text-amber-600" },
  ASSIGNED: { tone: "default", dotClass: "bg-indigo-500", iconClass: "text-indigo-600" },
  IN_PROGRESS: { tone: "default", dotClass: "bg-sky-500", iconClass: "text-sky-600" },
  RESOLVED: { tone: "success", dotClass: "bg-emerald-500", iconClass: "text-emerald-600" },
  ESCALATED: { tone: "destructive", dotClass: "bg-red-500", iconClass: "text-red-600" },
  CLOSED: { tone: "secondary", dotClass: "bg-slate-400", iconClass: "text-slate-500" },
  AUTO_CLOSED_PENDING: { tone: "destructive", dotClass: "bg-slate-500", iconClass: "text-slate-500" },
};

const PRIORITY_VISUALS: Record<string, PriorityVisual> = {
  CRITICAL: { tone: "destructive", borderClass: "border-l-red-500" },
  HIGH: { tone: "warning", borderClass: "border-l-orange-500" },
  MEDIUM: { tone: "default", borderClass: "border-l-amber-500" },
  LOW: { tone: "secondary", borderClass: "border-l-slate-400" },
};

export function getTicketStatusTone(status: string): StatusTone {
  const visual = STATUS_VISUALS[status.toUpperCase()];
  if (!visual) {
    return "default";
  }

  return visual.tone;
}

export function getTicketStatusDotClass(status: string): string {
  const visual = STATUS_VISUALS[status.toUpperCase()];
  if (!visual) {
    return "bg-slate-400";
  }

  return visual.dotClass;
}

export function getTicketStatusIconClass(status: string): string {
  const visual = STATUS_VISUALS[status.toUpperCase()];
  if (!visual) {
    return "text-slate-500";
  }

  return visual.iconClass;
}

export function getTicketPriorityTone(priority: string | null | undefined): StatusTone {
  if (!priority) {
    return "secondary";
  }

  const visual = PRIORITY_VISUALS[priority.toUpperCase()];
  if (!visual) {
    return "secondary";
  }

  return visual.tone;
}

export function getTicketPriorityBorderClass(priority: string | null | undefined): string {
  if (!priority) {
    return "border-l-slate-400";
  }

  const visual = PRIORITY_VISUALS[priority.toUpperCase()];
  if (!visual) {
    return "border-l-slate-400";
  }

  return visual.borderClass;
}
