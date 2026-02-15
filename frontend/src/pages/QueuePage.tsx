import { useLoaderData, useRevalidator, useNavigate } from "react-router-dom";
import { useEffect, useMemo, useState } from "react";
import type { QueueLoaderData } from "@/router";
import { formatLabel, formatRelativeTime, getPriorityTone, getStatusTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Inbox, Clock, User, Search } from "lucide-react";

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
              <Badge variant="outline" className="text-xs">
                {queueLabel}
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

export default function QueuePage() {
  const data = useLoaderData() as QueueLoaderData;
  const revalidator = useRevalidator();
  const queue = window.location.pathname.split("/").pop() || "GENERAL_Q";
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [queueFilter, setQueueFilter] = useState("ALL");

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  const queueTitle = queue === "ALL" ? "All Queues" : formatLabel(queue);
  const tickets = data?.content ?? [];
  const statusOptions = useMemo(
    () =>
      Array.from(new Set(tickets.map((ticket) => ticket.status).filter((status): status is string => Boolean(status)))).sort(),
    [tickets]
  );
  const priorityOptions = useMemo(
    () =>
      Array.from(
        new Set(tickets.map((ticket) => ticket.priority).filter((priority): priority is string => Boolean(priority)))
      ).sort(),
    [tickets]
  );
  const queueOptions = useMemo(
    () =>
      Array.from(new Set(tickets.map((ticket) => ticket.queue).filter((queueCode): queueCode is string => Boolean(queueCode)))).sort(),
    [tickets]
  );
  const normalizedSearch = searchTerm.trim().toLowerCase();
  const filteredTickets = useMemo(
    () =>
      tickets.filter((ticket) => {
        if (statusFilter !== "ALL" && ticket.status !== statusFilter) {
          return false;
        }
        if (priorityFilter !== "ALL" && ticket.priority !== priorityFilter) {
          return false;
        }
        if (queueFilter !== "ALL" && ticket.queue !== queueFilter) {
          return false;
        }
        if (!normalizedSearch) {
          return true;
        }
        const haystack = [ticket.formattedTicketNo, ticket.subject, ticket.customerName ?? "", ticket.assignedAgentName ?? ""]
          .join(" ")
          .toLowerCase();
        return haystack.includes(normalizedSearch);
      }),
    [tickets, statusFilter, priorityFilter, queueFilter, normalizedSearch]
  );
  const hasFilters = Boolean(normalizedSearch) || statusFilter !== "ALL" || priorityFilter !== "ALL" || queueFilter !== "ALL";
  const showEmptyState = data !== undefined && filteredTickets.length === 0;

  const renderTicketList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Inbox}
              title={hasFilters ? "No matching tickets" : "Queue is empty"}
              description={
                hasFilters
                  ? "Try adjusting the filters to see more tickets."
                  : "All tickets in this queue have been processed. Great work!"
              }
            />
          </CardContent>
        </Card>
      );
    }

    return filteredTickets.map((ticket) => (
      <QueueTicketCard key={ticket.id} ticket={ticket} navigatePath={`/app/agent/tickets/${ticket.id}`} />
    ));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={queueTitle}
        description="Tickets awaiting action in this queue"
      />

      <Card>
        <CardContent className="grid gap-3 p-4 md:grid-cols-2 lg:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Search ticket no, subject, customer, agent"
              className="pl-9"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger>
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Statuses</SelectItem>
              {statusOptions.map((status) => (
                <SelectItem key={status} value={status}>
                  {formatLabel(status)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={priorityFilter} onValueChange={setPriorityFilter}>
            <SelectTrigger>
              <SelectValue placeholder="Priority" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Priorities</SelectItem>
              {priorityOptions.map((priority) => (
                <SelectItem key={priority} value={priority}>
                  {formatLabel(priority)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={queueFilter} onValueChange={setQueueFilter}>
            <SelectTrigger>
              <SelectValue placeholder="Queue" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Queues</SelectItem>
              {queueOptions.map((queueCode) => (
                <SelectItem key={queueCode} value={queueCode}>
                  {formatLabel(queueCode)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {data && (
        <div className="flex items-center gap-4 rounded-lg bg-muted/50 px-4 py-2 text-sm">
          <div className="flex items-center gap-2">
            <Inbox className="h-4 w-4 text-muted-foreground" />
            <span className="font-medium">{filteredTickets.length}</span>
            <span className="text-muted-foreground">shown</span>
            <span className="text-muted-foreground">of</span>
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
