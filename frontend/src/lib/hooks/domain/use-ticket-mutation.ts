import { useApiMutation } from "@/lib/hooks/use-api-mutation";
import { api } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

type CreateTicketVariables = {
  subject: string;
  content: string;
};

type CreateTicketResponse = {
  id: number;
};

type AddReplyVariables = {
  ticketId: number;
  content: string;
};

type UpdateStatusVariables = {
  ticketId: number;
  newStatus: string;
  reason?: string;
};

type AssignSelfVariables = {
  ticketId: number;
};

type AssignAgentVariables = {
  ticketId: number;
  agentId: number;
  agentName?: string;
};

type ReleaseAgentVariables = {
  ticketId: number;
};

type ResolveEscalationVariables = {
  escalationId: number;
  resolutionNotes: string;
};

export function useCreateTicketMutation() {
  return useApiMutation({
    mutationFn: async (variables: CreateTicketVariables) => {
      const response = await api.post<CreateTicketResponse>(endpoints.tickets.create, variables);
      return response.data;
    },
    onSuccessMessage: "Ticket created successfully",
    onErrorMessage: "Failed to create ticket. Please try again.",
    revalidate: false,
  });
}

export function useAddReplyMutation() {
  return useApiMutation({
    mutationFn: (variables: AddReplyVariables) =>
      api.post(endpoints.tickets.replies(variables.ticketId), { content: variables.content }),
    onSuccessMessage: "Reply sent",
    onErrorMessage: "Failed to send reply",
    revalidate: true,
  });
}

export function useUpdateTicketStatusMutation() {
  return useApiMutation({
    mutationFn: (variables: UpdateStatusVariables) =>
      api.patch(endpoints.tickets.status(variables.ticketId), {
        newStatus: variables.newStatus,
        reason: variables.reason,
      }),
    onSuccessMessage: (_, vars) => `Ticket status changed to ${vars.newStatus}`,
    onErrorMessage: "Failed to change ticket status",
    revalidate: true,
  });
}

export function useAssignSelfMutation() {
  return useApiMutation({
    mutationFn: (variables: AssignSelfVariables) =>
      api.patch(endpoints.tickets.assignSelf(variables.ticketId)),
    onSuccessMessage: "Ticket assigned to you",
    onErrorMessage: "Failed to assign ticket",
    revalidate: true,
  });
}

export function useAssignAgentMutation() {
  return useApiMutation({
    mutationFn: (variables: AssignAgentVariables) =>
      api.patch(endpoints.tickets.assignAgent(variables.ticketId), { agentId: variables.agentId }),
    onSuccessMessage: (_, vars) => vars.agentName ? `Assigned to ${vars.agentName}` : "Ticket assignment updated",
    onErrorMessage: "Failed to assign ticket",
    revalidate: true,
  });
}

export function useReleaseAgentMutation() {
  return useApiMutation({
    mutationFn: (variables: ReleaseAgentVariables) =>
      api.patch(endpoints.tickets.releaseAgent(variables.ticketId)),
    onSuccessMessage: "Ticket unassigned",
    onErrorMessage: "Failed to unassign ticket",
    revalidate: true,
  });
}

export function useResolveEscalationMutation() {
  return useApiMutation({
    mutationFn: (variables: ResolveEscalationVariables) =>
      api.post(endpoints.supervisor.resolveEscalation(variables.escalationId), {
        resolutionNotes: variables.resolutionNotes,
      }),
    onSuccessMessage: "Escalation resolved",
    onErrorMessage: "Failed to resolve escalation",
    revalidate: true,
  });
}
