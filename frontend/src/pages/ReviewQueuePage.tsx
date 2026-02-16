import { useLoaderData, useRevalidator } from "react-router-dom";
import { useEffect, useMemo, useState } from "react";
import type { ReviewQueueLoaderData } from "@/router";
import { formatLabel, getStatusTone, formatRelativeTime, getPriorityTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { useNavigate } from "react-router-dom";
import { ClipboardCheck, Clock, Inbox, Search } from "lucide-react";

type ReviewTicketCardProps = Readonly<{
  ticket: ReviewQueueLoaderData["tickets"]["content"][0];
}>;

function ReviewTicketCard({ ticket }: ReviewTicketCardProps) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const queueLabel = ticket.queueLabel || ticket.queue || "-";
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

export default function ReviewQueuePage() {
  const data = useLoaderData<ReviewQueueLoaderData>();
  const revalidator = useRevalidator();
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

  const queueNameByCode = useMemo(
    () => new Map((data.metadata.queues ?? []).map((option) => [option.code, option.name])),
    [data.metadata.queues]
  );
  const tickets = data?.tickets.content ?? [];
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
    () => (data.metadata.queues ?? []).map((option) => option.code),
    [data.metadata.queues]
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
  const showEmptyState = filteredTickets.length === 0;

  const renderReviewList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={ClipboardCheck}
              title={hasFilters ? "No matching tickets" : "No tickets to review"}
              description={
                hasFilters
                  ? "Try adjusting the filters to see more tickets."
                  : "All AI routing decisions have been validated. You're all caught up!"
              }
            />
          </CardContent>
        </Card>
      );
    }

    return filteredTickets.map((ticket) => <ReviewTicketCard key={ticket.id} ticket={ticket} />);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Review Queue"
        description="Tickets requiring human review and validation"
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
                  {queueNameByCode.get(queueCode) || queueCode}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {data && (
        <div className="flex items-center gap-4 rounded-lg bg-amber-50 px-4 py-2 text-sm">
          <ClipboardCheck className="h-4 w-4 text-amber-600" />
          <div className="flex items-center gap-2">
            <span className="font-medium text-amber-700">{filteredTickets.length}</span>
            <span className="text-amber-600">shown</span>
            <span className="text-amber-600">of</span>
            <span className="font-medium text-amber-700">{data.tickets.totalElements}</span>
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
