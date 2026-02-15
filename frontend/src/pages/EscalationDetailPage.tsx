import { FormEvent, useState } from "react";
import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import type { EscalationDetailLoaderData } from "@/router";
import { api } from "@/lib/api";
import { formatDateTime, cn } from "@/lib/utils";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import { ArrowLeft, AlertTriangle, CheckCircle, Clock, User, Calendar, MessageSquare } from "lucide-react";
import { toast } from "sonner";

type DetailSectionProps = Readonly<{
  icon?: typeof AlertTriangle;
  title: string;
  children: React.ReactNode;
}>;

function DetailSection({ icon: Icon, title, children }: DetailSectionProps) {
  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{title}</p>
      </div>
      {children}
    </div>
  );
}

function getEscalationStatusMeta(resolved: boolean) {
  if (resolved) {
    return {
      badgeVariant: "success" as const,
      badgeIcon: CheckCircle,
      badgeLabel: "Resolved",
      leftBorderClass: "border-l-green-500",
      resolutionBorderClass: "border-l-green-500",
      resolutionIcon: CheckCircle,
      resolutionIconClass: "text-green-500",
      resolutionDescription: "This escalation has been resolved",
    };
  }

  return {
    badgeVariant: "destructive" as const,
    badgeIcon: AlertTriangle,
    badgeLabel: "Pending",
    leftBorderClass: "border-l-red-500",
    resolutionBorderClass: "border-l-amber-500",
    resolutionIcon: Clock,
    resolutionIconClass: "text-amber-500",
    resolutionDescription: "This escalation is pending resolution",
  };
}

export default function EscalationDetailPage() {
  const data = useLoaderData<EscalationDetailLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();
  const [resolutionNotes, setResolutionNotes] = useState("");
  const [resolving, setResolving] = useState(false);

  const statusMeta = getEscalationStatusMeta(data.resolved);
  const BadgeIcon = statusMeta.badgeIcon;
  const ResolutionIcon = statusMeta.resolutionIcon;

  const renderResolutionContent = () => {
    if (!data.resolutionNotes) {
      return (
        <div className="space-y-3">
          <div className="flex flex-col items-center justify-center py-4 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100">
              <Clock className="h-6 w-6 text-amber-600" />
            </div>
            <p className="mt-3 font-medium text-foreground">Waiting for Resolution</p>
            <p className="mt-1 text-sm text-muted-foreground">A supervisor will review this escalation soon</p>
          </div>
          <form onSubmit={(event: FormEvent) => {
            event.preventDefault();
            if (!resolutionNotes.trim() || resolving) {
              return;
            }

            setResolving(true);
            api.post(`/supervisor/escalations/${data.id}/resolve`, { resolutionNotes })
               .then(async () => {
                 await revalidator.revalidate();
                 setResolutionNotes("");
                 toast.success("Escalation resolved");
               })
               .catch((error) => {
                 toast.error("Failed to resolve escalation");
                 console.error("Failed to resolve escalation:", error);
               })
               .finally(() => setResolving(false));
          }} className="space-y-3">
            <Textarea
              placeholder="Resolution notes"
              value={resolutionNotes}
              onChange={(event) => setResolutionNotes(event.target.value)}
              rows={4}
            />
            <Button type="submit" className="w-full" disabled={!resolutionNotes.trim() || resolving}>
              {resolving ? "Resolving..." : "Resolve Escalation"}
            </Button>
          </form>
        </div>
      );
    }

    return (
      <>
        <DetailSection title="Resolution Notes">
          <div className="rounded-lg bg-green-50 p-3">
            <p className="text-sm leading-relaxed text-green-900">{data.resolutionNotes}</p>
          </div>
        </DetailSection>

        {data.resolvedAt && (
          <>
            <Separator />
            <div className="grid grid-cols-2 gap-4">
              <DetailSection icon={Calendar} title="Resolved At">
                <p className="text-sm font-medium">{formatDateTime(data.resolvedAt)}</p>
              </DetailSection>
              <DetailSection icon={User} title="Resolved By">
                <p className="text-sm font-medium">{data.resolvedBy || "-"}</p>
              </DetailSection>
            </div>
          </>
        )}
      </>
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="shrink-0">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm text-muted-foreground">{data.formattedTicketNo}</span>
            <Badge variant={statusMeta.badgeVariant} className="gap-1">
              <BadgeIcon className="h-3 w-3" />
              {statusMeta.badgeLabel}
            </Badge>
          </div>
          <h1 className="mt-1 text-xl font-bold text-foreground">Escalation Details</h1>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className={cn("border-l-4", statusMeta.leftBorderClass)}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <AlertTriangle className="h-4 w-4 text-red-500" />
              Escalation Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <DetailSection title="Reason for Escalation">
              <div className="rounded-lg bg-muted/50 p-3">
                <p className="text-sm leading-relaxed">{data.reason}</p>
              </div>
            </DetailSection>

            <Separator />

            <div className="grid grid-cols-2 gap-4">
              <DetailSection icon={Calendar} title="Created">
                <p className="text-sm font-medium">{formatDateTime(data.createdAt)}</p>
              </DetailSection>
              <DetailSection icon={User} title="Assigned Supervisor">
                <p className="text-sm font-medium">{data.assignedSupervisor || "-"}</p>
              </DetailSection>
            </div>
          </CardContent>
        </Card>

        <Card className={cn("border-l-4", statusMeta.resolutionBorderClass)}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <ResolutionIcon className={cn("h-4 w-4", statusMeta.resolutionIconClass)} />
              Resolution
            </CardTitle>
            <CardDescription>
              {statusMeta.resolutionDescription}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {renderResolutionContent()}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex items-center justify-between py-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <MessageSquare className="h-4 w-4" />
            <span>Need to view the original ticket?</span>
          </div>
          <Button variant="outline" onClick={() => navigate(`/app/agent/tickets/${data.ticketId}`)}>
            View Ticket
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
