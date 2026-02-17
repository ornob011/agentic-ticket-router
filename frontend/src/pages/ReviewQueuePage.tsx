import { useLoaderData, useRevalidator, useRouteLoaderData } from "react-router-dom";
import type { ReviewQueueLoaderData, RootLoaderData } from "@/router";
import { canAccessSupervisorWorkspace } from "@/lib/role-policy";
import { useAssignableAgents, useAssignAgentMutation, usePeriodicRevalidation, useReleaseAgentMutation } from "@/lib/hooks";
import { useTicketAssigneeSelection } from "@/lib/hooks/use-ticket-assignee-selection";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { TicketWorklist } from "@/components/ui/ticket-worklist";
import { ClipboardCheck } from "lucide-react";

export default function ReviewQueuePage() {
  const data = useLoaderData<ReviewQueueLoaderData>();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const revalidator = useRevalidator();
  const queueMetadataOptions = useQueueMetadataOptions();
  const assignAgentMutation = useAssignAgentMutation();
  const releaseAgentMutation = useReleaseAgentMutation();

  usePeriodicRevalidation(revalidator);

  const canAssignOthers = canAccessSupervisorWorkspace(appData?.user?.role);
  const { assignableAgents, reloadAssignableAgents } = useAssignableAgents(canAssignOthers);
  const tickets = data?.content ?? [];
  const { selectedAssignees, setSelectedAssignee } = useTicketAssigneeSelection(
    tickets,
    assignableAgents
  );

  const handleAssignAgent = async (ticketId: number, agentId: number) => {
    const agent = assignableAgents.find((a) => a.id === agentId);
    await assignAgentMutation.mutateAsync({
      ticketId,
      agentId,
      agentName: agent?.fullName || agent?.username,
    });
    await reloadAssignableAgents();
  };

  const handleUnassignAgent = async (ticketId: number) => {
    await releaseAgentMutation.mutateAsync({ ticketId });
    await reloadAssignableAgents();
  };

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
      canAssignOthers={canAssignOthers}
      assignableAgents={assignableAgents}
      selectedAssignees={selectedAssignees}
      onSelectedAssigneeChange={setSelectedAssignee}
      onAssignAgent={handleAssignAgent}
      onUnassignAgent={handleUnassignAgent}
      isAssignAgentPending={assignAgentMutation.isPending}
      isUnassignPending={releaseAgentMutation.isPending}
    />
  );
}
