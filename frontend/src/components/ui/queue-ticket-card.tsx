import { useState } from "react";
import { useNavigate } from "react-router-dom";
import type { AssignableAgentOption, TicketSummary } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { AgentSelectWithWorkload } from "@/components/ui/agent-select-with-workload";
import { formatLabel, formatRelativeTime, getPriorityTone, getStatusTone, cn } from "@/lib/utils";
import { getTicketPriorityBorderClass, getTicketStatusDotClass } from "@/lib/ticket-visuals";
import { Clock, Inbox, User, UserCheck, Loader2 } from "lucide-react";

type QueueTicketCardProps = Readonly<{
  ticket: TicketSummary;
  navigatePath: string;
  showCustomerName?: boolean;
  showNeedsReviewBadge?: boolean;
  canAssignOthers?: boolean;
  assignableAgents?: AssignableAgentOption[];
  selectedAgentId?: string;
  onSelectedAgentChange?: (agentId: string) => void;
  onAssignAgent?: () => Promise<void>;
  onUnassignAgent?: () => Promise<void>;
  isAssignAgentPending?: boolean;
  isUnassignPending?: boolean;
}>;

export function QueueTicketCard({
  ticket,
  navigatePath,
  showCustomerName = false,
  showNeedsReviewBadge = false,
  canAssignOthers = false,
  assignableAgents = [],
  selectedAgentId = "",
  onSelectedAgentChange,
  onAssignAgent,
  onUnassignAgent,
  isAssignAgentPending = false,
  isUnassignPending = false,
}: QueueTicketCardProps) {
  const [isUnassignDialogOpen, setIsUnassignDialogOpen] = useState(false);
  const navigate = useNavigate();
  const statusLabel = ticket.statusLabel || formatLabel(ticket.status);
  const priorityLabel = ticket.priorityLabel || formatLabel(ticket.priority);
  const priorityBorderClass = getTicketPriorityBorderClass(ticket.priority);

  return (
    <div
      className={cn(
        "group w-full rounded-lg border border-l-4 bg-card p-4 text-left transition-all hover:border-t-primary/30 hover:bg-accent/50 hover:shadow-sm",
        priorityBorderClass
      )}
    >
      <button
        type="button"
        onClick={() => navigate(navigatePath)}
        className="flex w-full items-stretch gap-4 text-left"
      >
        <div className="flex items-center gap-3">
          <div className={cn("h-2.5 w-2.5 shrink-0 rounded-full", getTicketStatusDotClass(ticket.status))} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-xs text-muted-foreground">{ticket.formattedTicketNo}</span>
            <Badge variant={getStatusTone(ticket.status)} className="text-xs">
              {statusLabel}
            </Badge>
            <Badge variant={getPriorityTone(ticket.priority)} className="text-xs">
              {priorityLabel}
            </Badge>
            {showNeedsReviewBadge && (
              <Badge variant="outline" className="border-amber-200 bg-amber-50 text-xs text-amber-700">
                Needs Review
              </Badge>
            )}
          </div>
          <p className="mt-1.5 truncate font-medium text-foreground">{ticket.subject}</p>
          <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {formatRelativeTime(ticket.lastActivityAt)}
            </div>
            {ticket.assignedAgentName ? (
              <div className="flex items-center gap-1">
                <UserCheck className="h-3 w-3 text-green-600" />
                <span className="text-green-700 font-medium">{ticket.assignedAgentName}</span>
              </div>
            ) : (
              <Badge variant="outline" className="text-xs border-dashed text-muted-foreground">
                Unassigned
              </Badge>
            )}
            {showCustomerName && ticket.customerName && (
              <div className="flex items-center gap-1">
                <User className="h-3 w-3" />
                {ticket.customerName}
              </div>
            )}
            {ticket.queue && (
              <div className="flex items-center gap-1">
                <Inbox className="h-3 w-3" />
                <Badge variant="outline" className="text-xs">
                  {ticket.queueLabel}
                </Badge>
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
      {canAssignOthers && onSelectedAgentChange && onAssignAgent && (
        <div className="mt-3 flex flex-col gap-2 border-t pt-3 sm:flex-row sm:items-center">
          <AgentSelectWithWorkload
            agents={assignableAgents}
            value={selectedAgentId}
            onChange={onSelectedAgentChange}
            placeholder={ticket.assignedAgentName ? "Reassign to..." : "Assign to agent"}
            className="sm:flex-1 sm:max-w-xs"
          />
          <Button
            type="button"
            size="sm"
            onClick={(event) => {
              event.stopPropagation();
              void onAssignAgent();
            }}
            disabled={!selectedAgentId || isAssignAgentPending}
          >
            {isAssignAgentPending ? (
              <>
                <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                Assigning...
              </>
            ) : ticket.assignedAgentName ? (
              "Reassign"
            ) : (
              "Assign"
            )}
          </Button>
          {ticket.assignedAgentName && onUnassignAgent && (
            <Button
              type="button"
              size="sm"
              variant="destructive"
              onClick={(event) => {
                event.stopPropagation();
                setIsUnassignDialogOpen(true);
              }}
              disabled={isUnassignPending}
            >
              {isUnassignPending ? "Unassigning..." : "Unassign"}
            </Button>
          )}
        </div>
      )}
      {ticket.assignedAgentName && onUnassignAgent && (
        <Dialog open={isUnassignDialogOpen} onOpenChange={setIsUnassignDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Unassign Agent?</DialogTitle>
              <DialogDescription>
                This will remove the assignee from ticket {ticket.formattedTicketNo}.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsUnassignDialogOpen(false)}>
                Cancel
              </Button>
              <Button
                type="button"
                variant="destructive"
                disabled={isUnassignPending}
                onClick={async () => {
                  try {
                    await onUnassignAgent();
                    setIsUnassignDialogOpen(false);
                  } catch {
                  }
                }}
              >
                {isUnassignPending ? "Unassigning..." : "Confirm Unassign"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
