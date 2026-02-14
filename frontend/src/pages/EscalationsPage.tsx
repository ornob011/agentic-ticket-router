import { useLoaderData, useRevalidator } from "react-router-dom";
import { useEffect } from "react";
import type { EscalationsLoaderData } from "@/router";
import { formatDateTime, cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/ui/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, CheckCircle, Clock, User } from "lucide-react";

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

function EscalationCard({ escalation }: Readonly<{ escalation: EscalationsLoaderData["content"][0] }>) {
  const navigate = useNavigate();
  const meta = resolveEscalationCardMeta(escalation.resolved);

  return (
    <button
      onClick={() => navigate(`/app/supervisor/escalations/${escalation.id}`)}
      className={cn(
        "group flex w-full items-stretch gap-4 rounded-lg border border-l-4 bg-card p-4 text-left transition-all hover:border-t-primary/30 hover:bg-accent/50 hover:shadow-sm",
        meta.borderClass
      )}
    >
      <div className="flex items-center gap-3">
        {meta.icon}
      </div>
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

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  const escalations = data?.content ?? [];
  const pendingCount = escalations.filter((e) => !e.resolved).length;
  const resolvedCount = escalations.filter((e) => e.resolved).length;
  const hasContent = escalations.length > 0;
  const showEmptyState = data !== undefined && escalations.length === 0;

  const renderEscalationList = () => {
    if (showEmptyState) {
      return (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={CheckCircle}
              title="No escalations"
              description="All tickets are running smoothly. No escalations require your attention."
            />
          </CardContent>
        </Card>
      );
    }

    return escalations.map((escalation) => <EscalationCard key={escalation.id} escalation={escalation} />);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Escalations"
        description="Manage escalated tickets requiring supervisor attention"
      />

      {data && hasContent && (
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
              <span className="text-sm font-medium text-slate-700">Total</span>
            </div>
            <p className="mt-1 text-2xl font-bold text-slate-700">{data.totalElements}</p>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {renderEscalationList()}
      </div>
    </div>
  );
}
