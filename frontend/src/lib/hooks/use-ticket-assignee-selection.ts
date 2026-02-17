import { useEffect, useState } from "react";
import type { AssignableAgentOption, TicketSummary } from "@/lib/api";

const EMPTY_ASSIGNABLE_AGENTS: AssignableAgentOption[] = [];

function buildSelectionMap(
  tickets: TicketSummary[],
  assignableAgents: AssignableAgentOption[]
): Record<number, string> {
  const next: Record<number, string> = {};

  for (const ticket of tickets) {
    const matchedAgent = ticket.assignedAgentName
      ? assignableAgents.find(
          (agent) => (agent.fullName ?? agent.username) === ticket.assignedAgentName
        )
      : null;

    next[ticket.id] = matchedAgent ? String(matchedAgent.id) : "";
  }

  return next;
}

function isSameMap(
  previous: Record<number, string>,
  next: Record<number, string>
): boolean {
  const previousKeys = Object.keys(previous);
  const nextKeys = Object.keys(next);

  if (previousKeys.length !== nextKeys.length) {
    return false;
  }

  for (const key of nextKeys) {
    if (previous[Number(key)] !== next[Number(key)]) {
      return false;
    }
  }

  return true;
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
      return isSameMap(previous, next) ? previous : next;
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
