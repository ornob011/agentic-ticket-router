import { useLoaderData, useRevalidator, useParams, useRouteLoaderData } from "react-router-dom";
import { useMemo } from "react";
import type { QueueLoaderData, RootLoaderData } from "@/router";
import { canAccessSupervisorWorkspace } from "@/lib/role-policy";
import { useAssignableAgents, useAssignAgentMutation, usePeriodicRevalidation, useReleaseAgentMutation } from "@/lib/hooks";
import { useTicketAssigneeSelection } from "@/lib/hooks/use-ticket-assignee-selection";
import { useQueueMetadataOptions } from "@/lib/hooks/use-queue-metadata-options";
import { TicketWorklist } from "@/components/ui/ticket-worklist";
import { Inbox } from "lucide-react";

export default function QueuePage() {
  const data = useLoaderData<QueueLoaderData>();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const revalidator = useRevalidator();
  const { queue = "GENERAL_Q" } = useParams();
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

  const selectedQueueOption = useMemo(
    () => queueMetadataOptions.find((option) => option.code === queue),
    [queueMetadataOptions, queue]
  );
  const queueTitle = queue === "ALL" ? "All Queues" : selectedQueueOption?.name || "Queue";
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
