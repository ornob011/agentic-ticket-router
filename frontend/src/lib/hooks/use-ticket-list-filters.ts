import { useMemo } from "react";
import type { TicketSummary } from "@/lib/api";

type UseTicketListFiltersInput = Readonly<{
  tickets: TicketSummary[];
  searchTerm: string;
  statusFilter: string;
  priorityFilter: string;
  queueFilter: string;
}>;

export function useTicketListFilters({
  tickets,
  searchTerm,
  statusFilter,
  priorityFilter,
  queueFilter,
}: UseTicketListFiltersInput) {
  const statusOptions = useMemo(
    () =>
      Array.from(new Set(tickets.map((ticket) => ticket.status).filter((status): status is string => Boolean(status)))).sort(),
    [tickets]
  );

  const priorityOptions = useMemo(
    () =>
      Array.from(
        new Set(tickets.map((ticket) => ticket.priority).filter((priority): priority is string => Boolean(priority)))
      ).sort(),
    [tickets]
  );

  const normalizedSearch = searchTerm.trim().toLowerCase();

  const filteredTickets = useMemo(
    () =>
      tickets.filter((ticket) => {
        if (statusFilter !== "ALL" && ticket.status !== statusFilter) {
          return false;
        }
        if (priorityFilter !== "ALL" && ticket.priority !== priorityFilter) {
          return false;
        }
        if (queueFilter !== "ALL" && ticket.queue !== queueFilter) {
          return false;
        }
        if (!normalizedSearch) {
          return true;
        }

        const haystack = [ticket.formattedTicketNo, ticket.subject, ticket.customerName ?? "", ticket.assignedAgentName ?? ""]
          .join(" ")
          .toLowerCase();

        return haystack.includes(normalizedSearch);
      }),
    [tickets, statusFilter, priorityFilter, queueFilter, normalizedSearch]
  );

  const hasFilters = Boolean(normalizedSearch) || statusFilter !== "ALL" || priorityFilter !== "ALL" || queueFilter !== "ALL";

  return {
    statusOptions,
    priorityOptions,
    filteredTickets,
    hasFilters,
  };
}
