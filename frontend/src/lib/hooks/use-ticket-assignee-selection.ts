import { useEffect, useState } from "react";
import isEqual from "lodash-es/isEqual";
import type { AssignableAgentOption, TicketSummary } from "@/lib/api";

const EMPTY_ASSIGNABLE_AGENTS: AssignableAgentOption[] = [];

function findAgentByDisplayName(
  assignableAgents: AssignableAgentOption[],
  assignedAgentName: string | null
) {
  if (!assignedAgentName) {
    return null;
  }

  return assignableAgents.find(
    (agent) => (agent.fullName ?? agent.username) === assignedAgentName
  ) ?? null;
}

function buildSelectionMap(
  tickets: TicketSummary[],
  assignableAgents: AssignableAgentOption[]
): Record<number, string> {
  const next: Record<number, string> = {};

  for (const ticket of tickets) {
    const matchedAgent = findAgentByDisplayName(
      assignableAgents,
      ticket.assignedAgentName
    );

    next[ticket.id] = matchedAgent ? String(matchedAgent.id) : "";
  }

  return next;
}

export function useTicketAssigneeSelection(
  tickets: TicketSummary[],
  assignableAgents?: AssignableAgentOption[]
) {
  const [selectedAssignees, setSelectedAssignees] = useState<Record<number, string>>({});
  const resolvedAssignableAgents = assignableAgents ?? EMPTY_ASSIGNABLE_AGENTS;

  useEffect(() => {
    setSelectedAssignees((previous) => {
      const next = buildSelectionMap(
        tickets,
        resolvedAssignableAgents
      );
      return isEqual(previous, next) ? previous : next;
    });
  }, [tickets, resolvedAssignableAgents]);

  const setSelectedAssignee = (
    ticketId: number,
    agentId: string
  ) => {
    setSelectedAssignees((previous) => {
      if (previous[ticketId] === agentId) {
        return previous;
      }

      return {
        ...previous,
        [ticketId]: agentId,
      };
    });
  };

  return {
    selectedAssignees,
    setSelectedAssignee,
  };
}
