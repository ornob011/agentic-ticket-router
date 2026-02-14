import { useLoaderData, useRevalidator, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import type { QueueLoaderData } from "@/router";
import { formatLabel, formatRelativeTime, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Inbox, Clock, User } from "lucide-react";

function QueueTicketCard({ ticket, navigatePath }: Readonly<{ ticket: QueueLoaderData["content"][0]; navigatePath: string }>) {
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
          <span className="text-xs">{statusLabel}</span>
          <span className="text-xs">{priorityLabel}</span>
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

export default function QueuePage() {
  const data = useLoaderData() as QueueLoaderData;
  const revalidator = useRevalidator();
  const queue = window.location.pathname.split("/").pop() || "GENERAL_Q";

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  const queueTitle = formatLabel(queue);
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
