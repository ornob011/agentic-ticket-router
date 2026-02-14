import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import { useEffect } from "react";
import type { TicketsLoaderData } from "@/router";
import { formatLabel, formatRelativeTime, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Plus, Ticket, Clock } from "lucide-react";

type TicketCardProps = Readonly<{
  ticket: TicketsLoaderData["content"][0];
}>;

function TicketCard({ ticket }: TicketCardProps) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const queueLabel = ticket.queueLabel || formatLabel(ticket.queue);
  const priorityBorderClass = getTicketPriorityBorderClass(ticket.priority);

  return (
    <button
      onClick={() => navigate(`/app/tickets/${ticket.id}`)}
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
          <span className="text-xs text-muted-foreground">{statusLabel}</span>
          <span className="text-xs text-muted-foreground">{priorityLabel}</span>
        </div>
        <p className="mt-1.5 truncate font-medium text-foreground">{ticket.subject}</p>
        <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
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

export default function TicketsPage() {
  const data = useLoaderData<TicketsLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  const tickets = data?.content ?? [];
  const hasContent = tickets.length > 0;
  const showEmptyState = data !== undefined && tickets.length === 0;

  const renderTicketList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Ticket}
              title="No tickets yet"
              description="Create your first support ticket to get started with our AI-powered routing system."
              action={{
                label: "Create Ticket",
                icon: Plus,
                onClick: () => navigate("/app/tickets/new"),
              }}
            />
          </CardContent>
        </Card>
      );
    }

    return tickets.map((ticket) => <TicketCard key={ticket.id} ticket={ticket} />);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Tickets"
        description="Track and manage your support requests"
      >
        <Button onClick={() => navigate("/app/tickets/new")}>
          <Plus className="mr-2 h-4 w-4" />
          New Ticket
        </Button>
      </PageHeader>

      {data && hasContent && (
        <div className="flex items-center gap-4 rounded-lg bg-muted/50 px-4 py-2 text-sm">
          <div className="flex items-center gap-2">
            <Ticket className="h-4 w-4 text-muted-foreground" />
            <span className="font-medium">{data.totalElements}</span>
            <span className="text-muted-foreground">total tickets</span>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {renderTicketList()}
      </div>
    </div>
  );
}
