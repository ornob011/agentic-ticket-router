import { useLoaderData, useRevalidator, useParams } from "react-router-dom";
import { useMemo, useState } from "react";
import type { QueueLoaderData } from "@/router";
import { usePeriodicRevalidation } from "@/lib/hooks/use-periodic-revalidation";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { useTicketListFilters } from "@/lib/hooks/use-ticket-list-filters";
import { Card, CardContent } from "@/components/ui/card";
import { TicketListFilters } from "@/components/ui/ticket-list-filters";
import { QueueTicketCard } from "@/components/ui/queue-ticket-card";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Inbox } from "lucide-react";

export default function QueuePage() {
  const data = useLoaderData() as QueueLoaderData;
  const revalidator = useRevalidator();
  const { queue = "GENERAL_Q" } = useParams();
  const queueMetadataOptions = useQueueMetadataOptions();
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [queueFilter, setQueueFilter] = useState("ALL");

  usePeriodicRevalidation(revalidator);

  const selectedQueueOption = useMemo(
    () => queueMetadataOptions.find((option) => option.code === queue),
    [queueMetadataOptions, queue]
  );
  const queueTitle = queue === "ALL" ? "All Queues" : selectedQueueOption?.name || "Queue";
  const tickets = data?.content ?? [];
  const { statusOptions, priorityOptions, filteredTickets, hasFilters } = useTicketListFilters({
    tickets,
    searchTerm,
    statusFilter,
    priorityFilter,
    queueFilter,
  });
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
      <QueueTicketCard
        key={ticket.id}
        ticket={ticket}
        navigatePath={`/app/agent/tickets/${ticket.id}`}
        showCustomerName
      />
    ));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={queueTitle}
        description="Tickets awaiting action in this queue"
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
