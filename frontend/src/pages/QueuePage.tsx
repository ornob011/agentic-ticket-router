import { useLoaderData, useRevalidator, useParams } from "react-router-dom";
import { useMemo } from "react";
import type { QueueLoaderData } from "@/router";
import { usePeriodicRevalidation } from "@/lib/hooks/use-periodic-revalidation";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { TicketWorklist } from "@/components/ui/ticket-worklist";
import { Inbox } from "lucide-react";

export default function QueuePage() {
  const data = useLoaderData<QueueLoaderData>();
  const revalidator = useRevalidator();
  const { queue = "GENERAL_Q" } = useParams();
  const queueMetadataOptions = useQueueMetadataOptions();

  usePeriodicRevalidation(revalidator);

  const selectedQueueOption = useMemo(
    () => queueMetadataOptions.find((option) => option.code === queue),
    [queueMetadataOptions, queue]
  );
  const queueTitle = queue === "ALL" ? "All Queues" : selectedQueueOption?.name || "Queue";
  const tickets = data?.content ?? [];

  return (
    <TicketWorklist
      title={queueTitle}
      description="Tickets awaiting action in this queue"
      emptyIcon={Inbox}
      emptyTitle="Queue is empty"
      emptyDescription="All tickets in this queue have been processed. Great work!"
      emptyFilteredTitle="No matching tickets"
      emptyFilteredDescription="Try adjusting the filters to see more tickets."
      summaryIcon={Inbox}
      summaryClassName="flex items-center gap-4 rounded-lg bg-muted/50 px-4 py-2 text-sm"
      summaryCountClassName="font-medium"
      summaryTextClassName="text-muted-foreground"
      summarySuffixLabel="tickets in queue"
      tickets={tickets}
      totalElements={data?.totalElements ?? 0}
      queueOptions={queueMetadataOptions}
      showCustomerName
    />
  );
}
