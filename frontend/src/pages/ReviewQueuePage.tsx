import { useLoaderData, useRevalidator } from "react-router-dom";
import type { ReviewQueueLoaderData } from "@/router";
import { usePeriodicRevalidation } from "@/lib/hooks/use-periodic-revalidation";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { TicketWorklist } from "@/components/ui/ticket-worklist";
import { ClipboardCheck } from "lucide-react";

export default function ReviewQueuePage() {
  const data = useLoaderData<ReviewQueueLoaderData>();
  const revalidator = useRevalidator();
  const queueMetadataOptions = useQueueMetadataOptions();

  usePeriodicRevalidation(revalidator);

  const tickets = data?.content ?? [];

  return (
    <TicketWorklist
      title="Review Queue"
      description="Tickets requiring human review and validation"
      emptyIcon={ClipboardCheck}
      emptyTitle="No tickets to review"
      emptyDescription="All AI routing decisions have been validated. You're all caught up!"
      emptyFilteredTitle="No matching tickets"
      emptyFilteredDescription="Try adjusting the filters to see more tickets."
      summaryIcon={ClipboardCheck}
      summaryClassName="flex items-center gap-4 rounded-lg bg-amber-50 px-4 py-2 text-sm"
      summaryCountClassName="font-medium text-amber-700"
      summaryTextClassName="text-amber-600"
      summarySuffixLabel="tickets awaiting review"
      tickets={tickets}
      totalElements={data?.totalElements ?? 0}
      queueOptions={queueMetadataOptions}
      showNeedsReviewBadge
    />
  );
}
