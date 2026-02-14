import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, type PagedResponse, type TicketSummary } from "@/lib/api";
import { formatLabel, getStatusTone, formatRelativeTime, getPriorityTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { ClipboardCheck, Clock, AlertTriangle } from "lucide-react";

type ReviewTicketCardProps = Readonly<{
  ticket: TicketSummary;
}>;

function ReviewTicketCard({ ticket }: ReviewTicketCardProps) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const queueLabel = ticket.queueLabel || formatLabel(ticket.queue);
  const priorityBorderClass = getTicketPriorityBorderClass(ticket.priority);

  return (
    <button
      onClick={() => navigate(`/app/agent/tickets/${ticket.id}`)}
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
          <Badge variant="outline" className="border-amber-200 bg-amber-50 text-xs text-amber-700">
            Needs Review
          </Badge>
        </div>
        <p className="mt-1.5 truncate font-medium text-foreground">{ticket.subject}</p>
        <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatRelativeTime(ticket.lastActivityAt)}
          </div>
          {ticket.queue && (
            <div className="flex items-center gap-1">
              <span className="font-medium">{queueLabel}</span>
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

function ReviewSkeleton() {
  return (
    <div className="space-y-6">
      <div className="gradient-header -mx-6 -mt-6 mb-6 px-6 py-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-2 h-4 w-64" />
      </div>
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-24" />
        ))}
      </div>
    </div>
  );
}

export default function ReviewQueuePage() {
  const { data, isLoading } = useQuery({
    queryKey: ["tickets", "review"],
    queryFn: async () => (await api.get<PagedResponse<TicketSummary>>("/tickets?scope=review&page=0&size=50")).data,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <ReviewSkeleton />;
  }

  const tickets = data?.content ?? [];
  const hasContent = tickets.length > 0;
  const showEmptyState = data !== undefined && tickets.length === 0;
  const renderReviewList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={ClipboardCheck}
              title="No tickets to review"
              description="All AI routing decisions have been validated. You're all caught up!"
            />
          </CardContent>
        </Card>
      );
    }

    return tickets.map((ticket) => <ReviewTicketCard key={ticket.id} ticket={ticket} />);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Review Queue"
        description="Tickets requiring human review and validation"
      />

      {data && hasContent && (
        <div className="flex items-center gap-4 rounded-lg bg-amber-50 px-4 py-2 text-sm">
          <AlertTriangle className="h-4 w-4 text-amber-600" />
          <div className="flex items-center gap-2">
            <span className="font-medium text-amber-700">{data.totalElements}</span>
            <span className="text-amber-600">tickets awaiting review</span>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {renderReviewList()}
      </div>
    </div>
  );
}
