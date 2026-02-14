import { useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, type PagedResponse, type TicketSummary } from "@/lib/api";
import { formatLabel, getStatusTone, formatRelativeTime, getPriorityTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Inbox, Clock, User } from "lucide-react";

function QueueTicketCard({ ticket, navigatePath }: Readonly<{ ticket: TicketSummary; navigatePath: string }>) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const queueLabel = ticket.queueLabel || formatLabel(ticket.queue);
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
        </div>
        <p className="mt-1.5 truncate font-medium text-foreground">{ticket.subject}</p>
        <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatRelativeTime(ticket.lastActivityAt)}
          </div>
          {ticket.customerName && (
            <div className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {ticket.customerName}
            </div>
          )}
          {ticket.queue && (
            <div className="flex items-center gap-1">
              <Inbox className="h-3 w-3" />
              {queueLabel}
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

function QueueSkeleton() {
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

export default function QueuePage() {
  const { queue } = useParams();

  const { data, isLoading } = useQuery({
    queryKey: ["tickets", "queue", queue],
    queryFn: async () =>
      (await api.get<PagedResponse<TicketSummary>>(`/tickets?scope=queue&queue=${queue}&page=0&size=50`)).data,
    enabled: Boolean(queue),
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <QueueSkeleton />;
  }

  const queueTitle = queue ? formatLabel(queue) : "Queue Inbox";
  const tickets = data?.content ?? [];
  const hasContent = tickets.length > 0;
  const showEmptyState = data !== undefined && tickets.length === 0;
  const renderTicketList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Inbox}
              title="Queue is empty"
              description="All tickets in this queue have been processed. Great work!"
            />
          </CardContent>
        </Card>
      );
    }

    return tickets.map((ticket) => (
      <QueueTicketCard key={ticket.id} ticket={ticket} navigatePath={`/app/agent/tickets/${ticket.id}`} />
    ));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={queueTitle}
        description="Tickets awaiting action in this queue"
      />

      {data && hasContent && (
        <div className="flex items-center gap-4 rounded-lg bg-muted/50 px-4 py-2 text-sm">
          <div className="flex items-center gap-2">
            <Inbox className="h-4 w-4 text-muted-foreground" />
            <span className="font-medium">{data.totalElements}</span>
            <span className="text-muted-foreground">tickets in queue</span>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {renderTicketList()}
      </div>
    </div>
  );
}
