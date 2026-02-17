import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import { useCallback, useMemo, useState } from "react";
import type { EscalationsLoaderData } from "@/router";
import { usePeriodicRevalidation } from "@/lib/hooks";
import { appRoutes } from "@/lib/routes";
import { EscalationsScreen, type EscalationStatusFilter } from "@/widgets/escalations/escalations-screen";

export default function EscalationsPage() {
  const data = useLoaderData<EscalationsLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();
  const isRefreshing = revalidator.state === "loading";

  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<EscalationStatusFilter>("ALL");
  const [assigneeFilter, setAssigneeFilter] = useState<string>("ALL");

  const escalations = useMemo(() => data?.content ?? [], [data]);

  usePeriodicRevalidation(revalidator);

  const refreshEscalations = useCallback(() => {
    void revalidator.revalidate();
  }, [revalidator]);

  const assigneeOptions = useMemo(
    () =>
      Array.from(
        new Set(
          escalations
            .map((escalation) => escalation.assignedSupervisor)
            .filter((name): name is string => Boolean(name))
        )
      ).sort(),
    [escalations]
  );

  const normalizedSearch = searchTerm.trim().toLowerCase();

  const filteredEscalations = useMemo(
    () =>
      escalations.filter((escalation) => {
        if (statusFilter === "PENDING" && escalation.resolved) {
          return false;
        }

        if (statusFilter === "RESOLVED" && !escalation.resolved) {
          return false;
        }

        if (assigneeFilter !== "ALL" && (escalation.assignedSupervisor ?? "") !== assigneeFilter) {
          return false;
        }

        if (!normalizedSearch) {
          return true;
        }

        const haystack = [escalation.formattedTicketNo, escalation.reason, escalation.assignedSupervisor ?? ""]
          .join(" ")
          .toLowerCase();

        return haystack.includes(normalizedSearch);
      }),
    [assigneeFilter, escalations, normalizedSearch, statusFilter]
  );

  const hasFilters = Boolean(normalizedSearch) || statusFilter !== "ALL" || assigneeFilter !== "ALL";

  const escalationStats = useMemo(() => {
    return filteredEscalations.reduce(
      (stats, escalation) => {
        if (escalation.resolved) {
          stats.resolved += 1;
        } else {
          stats.pending += 1;
        }

        return stats;
      },
      { pending: 0, resolved: 0 }
    );
  }, [filteredEscalations]);

  const resetFilters = useCallback(() => {
    setSearchTerm("");
    setStatusFilter("ALL");
    setAssigneeFilter("ALL");
  }, []);

  return (
    <EscalationsScreen
      escalations={filteredEscalations}
      totalElements={data.totalElements}
      searchTerm={searchTerm}
      statusFilter={statusFilter}
      assigneeFilter={assigneeFilter}
      assigneeOptions={assigneeOptions}
      hasFilters={hasFilters}
      isRefreshing={isRefreshing}
      pendingCount={escalationStats.pending}
      resolvedCount={escalationStats.resolved}
      onSearchChange={setSearchTerm}
      onStatusFilterChange={setStatusFilter}
      onAssigneeFilterChange={setAssigneeFilter}
      onRefresh={refreshEscalations}
      onClearFilters={resetFilters}
      onOpenEscalation={(escalationId) => void navigate(appRoutes.supervisor.escalationDetail(escalationId))}
    />
  );
}
