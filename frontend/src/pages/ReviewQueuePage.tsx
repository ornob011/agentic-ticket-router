import { useLoaderData, useRevalidator } from "react-router-dom";
import { useState } from "react";
import type { ReviewQueueLoaderData } from "@/router";
import { usePeriodicRevalidation } from "@/lib/hooks/use-periodic-revalidation";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { useTicketListFilters } from "@/lib/hooks/use-ticket-list-filters";
import { Card, CardContent } from "@/components/ui/card";
import { TicketListFilters } from "@/components/ui/ticket-list-filters";
import { QueueTicketCard } from "@/components/ui/queue-ticket-card";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { ClipboardCheck } from "lucide-react";

export default function ReviewQueuePage() {
  const data = useLoaderData<ReviewQueueLoaderData>();
  const revalidator = useRevalidator();
  const queueMetadataOptions = useQueueMetadataOptions();
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [queueFilter, setQueueFilter] = useState("ALL");

  usePeriodicRevalidation(revalidator);

  const tickets = data?.content ?? [];
  const { statusOptions, priorityOptions, filteredTickets, hasFilters } = useTicketListFilters({
    tickets,
    searchTerm,
    statusFilter,
    priorityFilter,
    queueFilter,
  });
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

    return filteredTickets.map((ticket) => (
      <QueueTicketCard
        key={ticket.id}
        ticket={ticket}
        navigatePath={`/app/agent/tickets/${ticket.id}`}
        showNeedsReviewBadge
      />
    ));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Review Queue"
        description="Tickets requiring human review and validation"
      />

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
        queueOptions={queueMetadataOptions}
      />

      {data && (
        <div className="flex items-center gap-4 rounded-lg bg-amber-50 px-4 py-2 text-sm">
          <ClipboardCheck className="h-4 w-4 text-amber-600" />
          <div className="flex items-center gap-2">
            <span className="font-medium text-amber-700">{filteredTickets.length}</span>
            <span className="text-amber-600">shown</span>
            <span className="text-amber-600">of</span>
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
