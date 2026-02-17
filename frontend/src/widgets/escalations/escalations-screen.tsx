import type { EscalationSummary } from "@/lib/api";
import { formatDateTime, cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { AlertTriangle, CheckCircle, Clock, Search, User } from "lucide-react";

export type EscalationStatusFilter = "ALL" | "PENDING" | "RESOLVED";

type EscalationsScreenProps = Readonly<{
  escalations: EscalationSummary[];
  totalElements: number;
  searchTerm: string;
  statusFilter: EscalationStatusFilter;
  assigneeFilter: string;
  assigneeOptions: string[];
  hasFilters: boolean;
  isRefreshing: boolean;
  pendingCount: number;
  resolvedCount: number;
  onSearchChange: (value: string) => void;
  onStatusFilterChange: (value: EscalationStatusFilter) => void;
  onAssigneeFilterChange: (value: string) => void;
  onRefresh: () => void;
  onClearFilters: () => void;
  onOpenEscalation: (escalationId: number) => void;
}>;

function resolveEscalationCardMeta(resolved: boolean) {
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
  onOpenEscalation,
}: Readonly<{
  escalation: EscalationSummary;
  onOpenEscalation: (escalationId: number) => void;
}>) {
  const meta = resolveEscalationCardMeta(escalation.resolved);

  return (
    <button
      type="button"
      onClick={() => onOpenEscalation(escalation.id)}
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

export function EscalationsScreen({
  escalations,
  totalElements,
  searchTerm,
  statusFilter,
  assigneeFilter,
  assigneeOptions,
  hasFilters,
  isRefreshing,
  pendingCount,
  resolvedCount,
  onSearchChange,
  onStatusFilterChange,
  onAssigneeFilterChange,
  onRefresh,
  onClearFilters,
  onOpenEscalation,
}: EscalationsScreenProps) {
  const showSummary = escalations.length > 0;
  const showEmptyState = escalations.length === 0;

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
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="Search ticket no, reason, supervisor"
              className="pl-9"
            />
          </div>

          <Select
            value={statusFilter}
            onValueChange={(value) => onStatusFilterChange(value as EscalationStatusFilter)}
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

          <Select value={assigneeFilter} onValueChange={onAssigneeFilterChange}>
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
            <Button type="button" variant="outline" onClick={onRefresh} disabled={isRefreshing}>
              {isRefreshing ? "Refreshing..." : "Refresh"}
            </Button>
            <Button type="button" variant="ghost" onClick={onClearFilters} disabled={!hasFilters}>
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
            <p className="mt-1 text-2xl font-bold text-red-700">{pendingCount}</p>
          </div>

          <div className="rounded-lg border border-green-200 bg-green-50 p-4">
            <div className="flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-green-600" />
              <span className="text-sm font-medium text-green-700">Resolved</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-green-700">{resolvedCount}</p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-slate-600" />
              <span className="text-sm font-medium text-slate-700">Shown / Total</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-slate-700">
              {escalations.length} / {totalElements}
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

        {!showEmptyState && escalations.map((escalation) => (
          <EscalationCard key={escalation.id} escalation={escalation} onOpenEscalation={onOpenEscalation} />
        ))}
      </div>
    </div>
  );
}
