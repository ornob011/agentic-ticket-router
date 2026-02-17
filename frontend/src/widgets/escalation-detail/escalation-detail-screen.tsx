import type { FormEvent } from "react";
import type { EscalationDetail } from "@/lib/api";
import { formatDateTime, cn } from "@/lib/utils";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import { DetailSection } from "@/components/ui/detail-section";
import { ArrowLeft, AlertTriangle, CheckCircle, Clock, User, Calendar, MessageSquare } from "lucide-react";

export type EscalationDetailScreenProps = Readonly<{
  escalation: EscalationDetail;
  resolutionNotes: string;
  isResolving: boolean;
  onResolutionNotesChange: (value: string) => void;
  onResolve: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onBack: () => void;
  onViewTicket: () => void;
}>;

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

export function EscalationDetailScreen({
  escalation,
  resolutionNotes,
  isResolving,
  onResolutionNotesChange,
  onResolve,
  onBack,
  onViewTicket,
}: EscalationDetailScreenProps) {
  const statusMeta = getEscalationStatusMeta(escalation.resolved);
  const BadgeIcon = statusMeta.badgeIcon;
  const ResolutionIcon = statusMeta.resolutionIcon;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack} className="shrink-0">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm text-muted-foreground">{escalation.formattedTicketNo}</span>
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
                <p className="text-sm leading-relaxed">{escalation.reason}</p>
              </div>
            </DetailSection>

            <Separator />

            <div className="grid grid-cols-2 gap-4">
              <DetailSection icon={Calendar} title="Created">
                <p className="text-sm font-medium">{formatDateTime(escalation.createdAt)}</p>
              </DetailSection>
              <DetailSection icon={User} title="Assigned Supervisor">
                <p className="text-sm font-medium">{escalation.assignedSupervisor || "-"}</p>
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
            {!escalation.resolutionNotes ? (
              <div className="space-y-3">
                <div className="flex flex-col items-center justify-center py-4 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100">
                    <Clock className="h-6 w-6 text-amber-600" />
                  </div>
                  <p className="mt-3 font-medium text-foreground">Waiting for Resolution</p>
                  <p className="mt-1 text-sm text-muted-foreground">A supervisor will review this escalation soon</p>
                </div>
                <form onSubmit={(event) => void onResolve(event)} className="space-y-3">
                  <Textarea
                    placeholder="Resolution notes"
                    value={resolutionNotes}
                    onChange={(event) => onResolutionNotesChange(event.target.value)}
                    rows={4}
                  />
                  <Button type="submit" className="w-full" disabled={!resolutionNotes.trim() || isResolving}>
                    {isResolving ? "Resolving..." : "Resolve Escalation"}
                  </Button>
                </form>
              </div>
            ) : (
              <>
                <DetailSection title="Resolution Notes">
                  <div className="rounded-lg bg-green-50 p-3">
                    <p className="text-sm leading-relaxed text-green-900">{escalation.resolutionNotes}</p>
                  </div>
                </DetailSection>

                {escalation.resolvedAt && (
                  <>
                    <Separator />
                    <div className="grid grid-cols-2 gap-4">
                      <DetailSection icon={Calendar} title="Resolved At">
                        <p className="text-sm font-medium">{formatDateTime(escalation.resolvedAt)}</p>
                      </DetailSection>
                      <DetailSection icon={User} title="Resolved By">
                        <p className="text-sm font-medium">{escalation.resolvedBy || "-"}</p>
                      </DetailSection>
                    </div>
                  </>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex items-center justify-between py-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <MessageSquare className="h-4 w-4" />
            <span>Need to view the original ticket?</span>
          </div>
          <Button variant="outline" onClick={onViewTicket}>
            View Ticket
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
