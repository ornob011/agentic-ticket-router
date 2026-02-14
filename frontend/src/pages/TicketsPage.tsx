import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, type PagedResponse, type TicketSummary } from "@/lib/api";
import { formatLabel, getStatusTone, formatRelativeTime, getPriorityTone, cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Plus, Ticket, Clock } from "lucide-react";

const statusColors: Record<string, string> = {
  RECEIVED: "bg-slate-400",
  TRIAGING: "bg-blue-400",
  WAITING_CUSTOMER: "bg-amber-400",
  ASSIGNED: "bg-indigo-400",
  IN_PROGRESS: "bg-sky-400",
  RESOLVED: "bg-green-400",
  ESCALATED: "bg-red-400",
  CLOSED: "bg-slate-400",
  AUTO_CLOSED_PENDING: "bg-slate-400",
};

const priorityColors: Record<string, string> = {
  CRITICAL: "border-l-red-500",
  HIGH: "border-l-orange-500",
  MEDIUM: "border-l-amber-500",
  LOW: "border-l-slate-400",
};

type TicketCardProps = Readonly<{
  ticket: TicketSummary;
}>;

function TicketCard({ ticket }: TicketCardProps) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const queueLabel = ticket.queueLabel || formatLabel(ticket.queue);
  const priorityBorderClass = ticket.priority ? priorityColors[ticket.priority] : undefined;
  const handleOpenTicket = () => {
    Promise.resolve(navigate(`/app/tickets/${ticket.id}`)).catch(() => undefined);
  };

  return (
    <button
      onClick={handleOpenTicket}
      className={cn(
        "group flex w-full items-stretch gap-4 rounded-lg border border-l-4 bg-card p-4 text-left transition-all hover:border-t-primary/30 hover:bg-accent/50 hover:shadow-sm",
        priorityBorderClass || "border-l-slate-400"
      )}
    >
      <div className="flex items-center gap-3">
        <div className={cn("h-2.5 w-2.5 shrink-0 rounded-full", statusColors[ticket.status] || "bg-slate-400")} />
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

function TicketsSkeleton() {
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

export default function TicketsPage() {
  const navigate = useNavigate();
  const handleCreateTicket = () => {
    Promise.resolve(navigate("/app/tickets/new")).catch(() => undefined);
  };

  const { data, isLoading } = useQuery({
    queryKey: ["tickets", "mine"],
    queryFn: async () => (await api.get<PagedResponse<TicketSummary>>("/tickets?scope=mine&page=0&size=20")).data,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <TicketsSkeleton />;
  }

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
                onClick: handleCreateTicket,
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
        <Button onClick={handleCreateTicket}>
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
