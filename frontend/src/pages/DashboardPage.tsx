import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import { useEffect } from "react";
import type { DashboardLoaderData } from "@/router";
import { formatLabel, getStatusTone, formatRelativeTime, getPriorityTone, cn } from "@/lib/utils";
import { getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { canAccessAgentWorkspace, canAccessSupervisorWorkspace } from "@/lib/role-policy";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { StatCard } from "@/components/ui/stat-card";
import { StatusDot } from "@/components/ui/status-dot";
import {
  Ticket,
  ArrowRight,
  Plus,
  CheckCircle,
  Clock,
  AlertTriangle,
  MessageSquare,
  Users,
  TrendingUp,
} from "lucide-react";

type DashboardStat = {
  label: string;
  value: number;
  color: "blue" | "orange" | "green" | "purple" | "red";
  icon: typeof Ticket;
};

function buildCustomerStats(data: DashboardLoaderData): DashboardStat[] {
  if (!data.customer) {
    return [];
  }

  return [
    { label: "Open Tickets", value: data.customer.openTickets ?? 0, color: "blue", icon: Ticket },
    { label: "Waiting On Me", value: data.customer.waitingOnMe ?? 0, color: "orange", icon: Clock },
    { label: "Resolved", value: data.customer.resolvedTickets ?? 0, color: "green", icon: CheckCircle },
    { label: "Closed", value: data.customer.closedTickets ?? 0, color: "purple", icon: TrendingUp },
  ];
}

function buildAgentStats(data: DashboardLoaderData): DashboardStat[] {
  if (!data.agent) {
    return [];
  }

  return [
    { label: "Assigned", value: data.agent.myAssignedCount ?? 0, color: "blue", icon: Ticket },
    { label: "General Queue", value: data.agent.queueGeneral ?? 0, color: "orange", icon: Clock },
    { label: "Tech Queue", value: data.agent.queueTech ?? 0, color: "green", icon: CheckCircle },
    { label: "Billing Queue", value: data.agent.queueBilling ?? 0, color: "purple", icon: MessageSquare },
  ];
}

function buildSupervisorStats(data: DashboardLoaderData): DashboardStat[] {
  if (!data.supervisor) {
    return [];
  }

  return [
    { label: "Escalations", value: data.supervisor.pendingEscalations ?? 0, color: "red", icon: AlertTriangle },
    { label: "SLA Breaches", value: data.supervisor.slaBreaches ?? 0, color: "orange", icon: Clock },
    { label: "Human Review", value: data.supervisor.humanReviewCount ?? 0, color: "blue", icon: Users },
  ];
}

function resolveStats(role: DashboardLoaderData["user"]["role"], data: DashboardLoaderData): DashboardStat[] {
  if (canAccessSupervisorWorkspace(role)) {
    return buildSupervisorStats(data);
  }

  if (role === "AGENT") {
    return buildAgentStats(data);
  }

  return buildCustomerStats(data);
}

function RecentTicketItem({ ticket }: Readonly<{ ticket: DashboardLoaderData["recentTickets"][0] }>) {
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);

  return (
    <button
      onClick={() => navigate(`/app/tickets/${ticket.id}`)}
      className="group flex w-full items-center gap-4 rounded-lg border bg-card p-4 text-left transition-all hover:border-primary/30 hover:bg-accent/50 hover:shadow-sm"
    >
      <div className={cn("h-2 w-2 shrink-0 rounded-full", getTicketStatusDotClass(ticket.status))} />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-mono text-xs text-muted-foreground">{ticket.formattedTicketNo}</span>
          <Badge variant={getStatusTone(ticket.status)} className="text-xs">
            {statusLabel}
          </Badge>
          {ticket.priority && (
            <Badge variant={getPriorityTone(ticket.priority)} className="text-xs">
              {priorityLabel}
            </Badge>
          )}
        </div>
        <p className="mt-1 truncate font-medium text-foreground">{ticket.subject}</p>
      </div>
      <div className="hidden items-center gap-2 text-xs text-muted-foreground sm:flex">
        <Clock className="h-3 w-3" />
        {formatRelativeTime(ticket.lastActivityAt)}
      </div>
      <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground/0 transition-all group-hover:text-muted-foreground" />
    </button>
  );
}

export default function DashboardPage() {
  const data = useLoaderData<DashboardLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  const role = data.user.role;
  const isAgent = canAccessAgentWorkspace(role);
  const isSupervisor = canAccessSupervisorWorkspace(role);
  const stats = resolveStats(role, data);
  const hasRecentTickets = data.recentTickets.length > 0;

  const renderRecentTickets = () => {
    if (!hasRecentTickets) {
      return (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
            <Ticket className="h-8 w-8 text-muted-foreground/60" />
          </div>
          <p className="mt-4 font-medium text-foreground">No tickets yet</p>
          <p className="mt-1 text-sm text-muted-foreground">Create your first ticket to get started</p>
          <Button className="mt-4" onClick={() => navigate("/app/tickets/new")}>
            <Plus className="mr-2 h-4 w-4" />
            Create Ticket
          </Button>
        </div>
      );
    }

    return (
      <div className="space-y-2">
        {data.recentTickets.map((ticket) => (
          <RecentTicketItem key={ticket.id} ticket={ticket} />
        ))}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description={`Welcome back, ${data.user.fullName || data.user.username}`}
      >
        <Button onClick={() => navigate("/app/tickets/new")}>
          <Plus className="mr-2 h-4 w-4" />
          New Ticket
        </Button>
      </PageHeader>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat, i) => (
          <StatCard key={i} label={stat.label} value={stat.value} icon={stat.icon} color={stat.color} />
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <Ticket className="h-4 w-4 text-primary" />
              Recent Tickets
            </CardTitle>
            <Button variant="ghost" size="sm" onClick={() => navigate("/app/tickets")}>
              View all
              <ArrowRight className="ml-1 h-3 w-3" />
            </Button>
          </CardHeader>
          <CardContent>
            {renderRecentTickets()}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <StatusDot status="online" pulse size="sm" />
              Quick Actions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <Button
              variant="outline"
              className="w-full justify-start"
              onClick={() => navigate("/app/tickets/new")}
            >
              <Plus className="mr-2 h-4 w-4" />
              Create New Ticket
            </Button>
            <Button
              variant="outline"
              className="w-full justify-start"
              onClick={() => navigate("/app/tickets")}
            >
              <Ticket className="mr-2 h-4 w-4" />
              View All Tickets
            </Button>
            {isAgent && (
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={() => navigate("/app/agent/queues/GENERAL_Q")}
              >
                <MessageSquare className="mr-2 h-4 w-4" />
                My Queue
              </Button>
            )}
            {isSupervisor && (
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={() => navigate("/app/supervisor/escalations")}
              >
                <AlertTriangle className="mr-2 h-4 w-4" />
                View Escalations
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
