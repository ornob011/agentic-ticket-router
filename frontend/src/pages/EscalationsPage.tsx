import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { EscalationsLoaderData } from "@/router";
import { formatDateTime, cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { AlertTriangle, CheckCircle, Clock, Search, User } from "lucide-react";

type EscalationStatusFilter = "ALL" | "PENDING" | "RESOLVED";
type AssigneeFilter = "ALL" | string;
type EscalationItem = EscalationsLoaderData["content"][number];

const AUTO_REFRESH_INTERVAL_MS = 30_000;

function resolveEscalationCardMeta(
  resolved: boolean
) {
  if (resolved) {
    return {
      borderClass: "border-l-green-500",
      badge: (
        <Badge variant="success" className="text-xs">
          Resolved
        </Badge>
      ),
      icon: (
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-green-100">
          <CheckCircle className="h-4 w-4 text-green-600" />
        </div>
      ),
    };
  }

  return {
    borderClass: "border-l-red-500",
    badge: (
      <Badge variant="destructive" className="text-xs">
        Pending
      </Badge>
    ),
    icon: (
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-red-100">
        <AlertTriangle className="h-4 w-4 text-red-600" />
      </div>
    ),
  };
}

function EscalationCard({
  escalation,
}: Readonly<{
  escalation: EscalationItem;
}>) {
  const navigate = useNavigate();
  const meta = resolveEscalationCardMeta(escalation.resolved);
  const detailsPath = `/app/supervisor/escalations/${escalation.id}`;

  const onOpenDetails = useCallback(() => {
    navigate(detailsPath);
  }, [navigate, detailsPath]);

  return (
    <button
      type="button"
      onClick={onOpenDetails}
      aria-label={`Open escalation ${escalation.formattedTicketNo}`}
      className={cn(
        "group flex w-full items-stretch gap-4 rounded-lg border border-l-4 bg-card p-4 text-left transition-all hover:border-t-primary/30 hover:bg-accent/50 hover:shadow-sm",
        meta.borderClass
      )}
    >
      <div className="flex items-center gap-3">{meta.icon}</div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-mono text-xs text-muted-foreground">{escalation.formattedTicketNo}</span>
          {meta.badge}
        </div>
        <p className="mt-1.5 truncate font-medium text-foreground">{escalation.reason}</p>
        <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatDateTime(escalation.createdAt)}
          </div>
          {escalation.assignedSupervisor && (
            <div className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {escalation.assignedSupervisor}
            </div>
          )}
        </div>
      </div>
      <div className="flex items-center">
        <div className="rounded-full bg-muted p-2 opacity-0 transition-opacity group-hover:opacity-100">
          <svg className="h-4 w-4 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </button>
  );
}

export default function EscalationsPage() {
  const data = useLoaderData<EscalationsLoaderData>();
  const revalidator = useRevalidator();
  const isRefreshing = revalidator.state === "loading";

  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<EscalationStatusFilter>("ALL");
  const [assigneeFilter, setAssigneeFilter] = useState<AssigneeFilter>("ALL");

  const escalations = data?.content ?? [];

  const refreshEscalations = useCallback(() => {
    revalidator.revalidate();
  }, [revalidator]);

  useEffect(() => {
    const interval = setInterval(() => {
      if (document.visibilityState === "visible") {
        refreshEscalations();
      }
    }, AUTO_REFRESH_INTERVAL_MS);

    return () => clearInterval(interval);
  }, [refreshEscalations]);

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

  const showSummary = data !== undefined && filteredEscalations.length > 0;
  const showEmptyState = data !== undefined && filteredEscalations.length === 0;

  const resetFilters = useCallback(() => {
    setSearchTerm("");
    setStatusFilter("ALL");
    setAssigneeFilter("ALL");
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Escalations"
        description="Manage escalated tickets requiring supervisor attention"
      />

      <Card>
        <CardContent className="grid gap-3 p-4 md:grid-cols-2 lg:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Search ticket no, reason, supervisor"
              className="pl-9"
            />
          </div>

          <Select
            value={statusFilter}
            onValueChange={(value) => setStatusFilter(value as EscalationStatusFilter)}
          >
            <SelectTrigger>
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All States</SelectItem>
              <SelectItem value="PENDING">Pending</SelectItem>
              <SelectItem value="RESOLVED">Resolved</SelectItem>
            </SelectContent>
          </Select>

          <Select
            value={assigneeFilter}
            onValueChange={(value) => setAssigneeFilter(value as AssigneeFilter)}
          >
            <SelectTrigger>
              <SelectValue placeholder="Assigned Supervisor" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Supervisors</SelectItem>
              {assigneeOptions.map((assignee) => (
                <SelectItem key={assignee} value={assignee}>
                  {assignee}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="flex items-center justify-end gap-2 md:col-span-2 lg:col-span-4">
            <Button
              type="button"
              variant="outline"
              onClick={refreshEscalations}
              disabled={isRefreshing}
            >
              {isRefreshing ? "Refreshing..." : "Refresh"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={resetFilters}
              disabled={!hasFilters}
            >
              Clear Filters
            </Button>
          </div>
        </CardContent>
      </Card>

      {showSummary && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div className="rounded-lg border border-red-200 bg-red-50 p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-red-600" />
              <span className="text-sm font-medium text-red-700">Pending</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-red-700">{escalationStats.pending}</p>
          </div>

          <div className="rounded-lg border border-green-200 bg-green-50 p-4">
            <div className="flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-green-600" />
              <span className="text-sm font-medium text-green-700">Resolved</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-green-700">{escalationStats.resolved}</p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-slate-600" />
              <span className="text-sm font-medium text-slate-700">Shown / Total</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-slate-700">
              {filteredEscalations.length} / {data.totalElements}
            </p>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {showEmptyState && (
          <Card>
            <CardContent className="p-0">
              <EmptyState
                icon={CheckCircle}
                title={hasFilters ? "No matching escalations" : "No escalations"}
                description={
                  hasFilters
                    ? "Try adjusting the filters to see more escalations."
                    : "All tickets are running smoothly. No escalations require your attention."
                }
              />
            </CardContent>
          </Card>
        )}

        {!showEmptyState && filteredEscalations.map((escalation) => (
          <EscalationCard key={escalation.id} escalation={escalation} />
        ))}
      </div>
    </div>
  );
}
