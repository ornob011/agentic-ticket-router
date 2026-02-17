import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import type { AssignableAgentOption, LookupOption, TicketSummary } from "@/lib/api";
import { useTicketListFilters } from "@/lib/hooks/use-ticket-list-filters";
import { appRoutes } from "@/lib/routes";
import { Card, CardContent } from "@/components/ui/card";
import { TicketListFilters } from "@/components/ui/ticket-list-filters";
import { QueueTicketCard } from "@/components/ui/queue-ticket-card";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";

type TicketWorklistProps = Readonly<{
  title: string;
  description: string;
  headerActions?: ReactNode;
  emptyIcon: LucideIcon;
  emptyTitle: string;
  emptyDescription: string;
  emptyFilteredTitle: string;
  emptyFilteredDescription: string;
  emptyAction?: {
    label: string;
    onClick: () => void;
    icon?: LucideIcon;
  };
  summaryIcon: LucideIcon;
  summaryClassName: string;
  summaryCountClassName: string;
  summaryTextClassName: string;
  summarySuffixLabel: string;
  summaryActions?: ReactNode;
  showSummary?: boolean;
  tickets: TicketSummary[];
  totalElements: number;
  queueOptions: LookupOption[];
  navigatePathBuilder?: (ticket: TicketSummary) => string;
  showFilters?: boolean;
  showCustomerName?: boolean;
  showNeedsReviewBadge?: boolean;
  canAssignOthers?: boolean;
  assignableAgents?: AssignableAgentOption[];
  onAssignAgent?: (ticketId: number, agentId: number) => Promise<void>;
  onUnassignAgent?: (ticketId: number) => Promise<void>;
  isAssignAgentPending?: boolean;
  isUnassignPending?: boolean;
}>;

export function TicketWorklist({
  title,
  description,
  headerActions,
  emptyIcon,
  emptyTitle,
  emptyDescription,
  emptyFilteredTitle,
  emptyFilteredDescription,
  emptyAction,
  summaryIcon: SummaryIcon,
  summaryClassName,
  summaryCountClassName,
  summaryTextClassName,
  summarySuffixLabel,
  summaryActions,
  showSummary = true,
  tickets,
  totalElements,
  queueOptions,
  navigatePathBuilder = (ticket) => appRoutes.agent.ticketDetail(ticket.id),
  showFilters = true,
  showCustomerName = false,
  showNeedsReviewBadge = false,
  canAssignOthers = false,
  assignableAgents = [],
  onAssignAgent,
  onUnassignAgent,
  isAssignAgentPending = false,
  isUnassignPending = false,
}: TicketWorklistProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [queueFilter, setQueueFilter] = useState("ALL");
  const [selectedAssignees, setSelectedAssignees] = useState<Record<number, string>>({});

  useEffect(() => {
    setSelectedAssignees((prev) => {
      const next = { ...prev };

      for (const ticket of tickets) {
        const matchedAgent = ticket.assignedAgentName
          ? assignableAgents.find(
              (agent) => (agent.fullName || agent.username) === ticket.assignedAgentName
            )
          : null;
        const nextValue = matchedAgent ? String(matchedAgent.id) : "";
        next[ticket.id] = nextValue;
      }

      return next;
    });
  }, [tickets, assignableAgents]);

  const { statusOptions, priorityOptions, filteredTickets: clientFilteredTickets, hasFilters } = useTicketListFilters({
    tickets,
    searchTerm,
    statusFilter,
    priorityFilter,
    queueFilter,
  });

  const filteredTickets = showFilters ? clientFilteredTickets : tickets;
  const showEmptyState = filteredTickets.length === 0;

  const renderTicketList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={emptyIcon}
              title={hasFilters ? emptyFilteredTitle : emptyTitle}
              description={hasFilters ? emptyFilteredDescription : emptyDescription}
              action={emptyAction}
            />
          </CardContent>
        </Card>
      );
    }

    return filteredTickets.map((ticket) => (
      <QueueTicketCard
        key={ticket.id}
        ticket={ticket}
        navigatePath={navigatePathBuilder(ticket)}
        showCustomerName={showCustomerName}
        showNeedsReviewBadge={showNeedsReviewBadge}
        canAssignOthers={canAssignOthers}
        assignableAgents={assignableAgents}
        selectedAgentId={selectedAssignees[ticket.id] || ""}
        onSelectedAgentChange={(agentId) =>
          setSelectedAssignees((prev) => ({
            ...prev,
            [ticket.id]: agentId,
          }))
        }
        onAssignAgent={async () => {
          if (!onAssignAgent) {
            return;
          }

          const selectedAgentId = selectedAssignees[ticket.id];
          if (!selectedAgentId) {
            return;
          }

          await onAssignAgent(ticket.id, Number(selectedAgentId));
        }}
        onUnassignAgent={
          onUnassignAgent
            ? async () => {
                await onUnassignAgent(ticket.id);
              }
            : undefined
        }
        isAssignAgentPending={isAssignAgentPending}
        isUnassignPending={isUnassignPending}
      />
    ));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={title}
        description={description}
      >
        {headerActions}
      </PageHeader>

      {showFilters && (
        <TicketListFilters
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          statusOptions={statusOptions}
          priorityFilter={priorityFilter}
          onPriorityFilterChange={setPriorityFilter}
          priorityOptions={priorityOptions}
          queueFilter={queueFilter}
          onQueueFilterChange={setQueueFilter}
          queueOptions={queueOptions}
        />
      )}

      {showSummary && (
        <div className={summaryClassName}>
          <SummaryIcon className={`h-4 w-4 ${summaryTextClassName}`} />
          <div className="flex items-center gap-2">
            <span className={summaryCountClassName}>{filteredTickets.length}</span>
            <span className={summaryTextClassName}>shown</span>
            <span className={summaryTextClassName}>of</span>
            <span className={summaryCountClassName}>{totalElements}</span>
            <span className={summaryTextClassName}>{summarySuffixLabel}</span>
          </div>
          {summaryActions}
        </div>
      )}

      <div className="space-y-3">
        {renderTicketList()}
      </div>
    </div>
  );
}
