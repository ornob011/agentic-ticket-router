import { useNavigate } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { formatLabel, formatRelativeTime, getPriorityTone, getStatusTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import type { TicketSummary } from "@/lib/api";
import { Clock, Inbox, User } from "lucide-react";

type QueueTicketCardProps = Readonly<{
  ticket: TicketSummary;
  navigatePath: string;
  showCustomerName?: boolean;
  showNeedsReviewBadge?: boolean;
}>;

export function QueueTicketCard({
  ticket,
  navigatePath,
  showCustomerName = false,
  showNeedsReviewBadge = false,
}: QueueTicketCardProps) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const priorityBorderClass = getTicketPriorityBorderClass(ticket.priority);

  return (
    <button
      onClick={() => navigate(navigatePath)}
      className={cn(
        "group flex w-full items-stretch gap-4 rounded-lg border border-l-4 bg-card p-4 text-left transition-all hover:border-t-primary/30 hover:bg-accent/50 hover:shadow-sm",
        priorityBorderClass
      )}
    >
      <div className="flex items-center gap-3">
        <div className={cn("h-2.5 w-2.5 shrink-0 rounded-full", getTicketStatusDotClass(ticket.status))} />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-mono text-xs text-muted-foreground">{ticket.formattedTicketNo}</span>
          <Badge variant={getStatusTone(ticket.status)} className="text-xs">
            {statusLabel}
          </Badge>
          <Badge variant={getPriorityTone(ticket.priority)} className="text-xs">
            {priorityLabel}
          </Badge>
          {showNeedsReviewBadge && (
            <Badge variant="outline" className="border-amber-200 bg-amber-50 text-xs text-amber-700">
              Needs Review
            </Badge>
          )}
        </div>
        <p className="mt-1.5 truncate font-medium text-foreground">{ticket.subject}</p>
        <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatRelativeTime(ticket.lastActivityAt)}
          </div>
          {showCustomerName && ticket.customerName && (
            <div className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {ticket.customerName}
            </div>
          )}
          {ticket.queue && (
            <div className="flex items-center gap-1">
              <Inbox className="h-3 w-3" />
              <Badge variant="outline" className="text-xs">
                {ticket.queueLabel}
              </Badge>
            </div>
          )}
        </div>
      </div>
      <div className="flex items-center">
        <div className="rounded-full bg-muted p-2 opacity-0 transition-opacity group-hover:opacity-100">
          <svg className="h-4 w-4 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </button>
  );
}
