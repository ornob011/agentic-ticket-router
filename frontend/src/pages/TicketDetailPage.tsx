import { FormEvent, useRef, useEffect, useState } from "react";
import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import type { TicketDetailLoaderData } from "@/router";
import { api, type TicketMessage } from "@/lib/api";
import { formatLabel, getStatusTone, formatDateTime, getPriorityTone, cn } from "@/lib/utils";
import { getTicketStatusIconClass } from "@/lib/ticket-visuals";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DateSeparator } from "@/components/ui/date-separator";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  ArrowLeft,
  Send,
  User,
  Sparkles,
  Clock,
  AlertCircle,
  CheckCircle,
  MessageSquare,
} from "lucide-react";
import { toast } from "sonner";

function formatDateGroup(dateStr: string): string {
  const date = new Date(dateStr);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  if (date.toDateString() === today.toDateString()) {
    return "Today";
  }

  if (date.toDateString() === yesterday.toDateString()) {
    return "Yesterday";
  }

  let yearFormat: "numeric" | undefined;
  if (date.getFullYear() !== today.getFullYear()) {
    yearFormat = "numeric";
  }

  return date.toLocaleDateString("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
    year: yearFormat,
  });
}

function formatMessageTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

type MessageGroup = {
  date: string;
  messages: TicketMessage[];
};

function groupMessagesByDate(messages: TicketMessage[]): MessageGroup[] {
  const groups: MessageGroup[] = [];
  let currentGroup: MessageGroup | null = null;

  for (const message of messages) {
    const dateGroup = formatDateGroup(message.createdAt);

    if (!currentGroup || currentGroup.date !== dateGroup) {
      currentGroup = { date: dateGroup, messages: [] };
      groups.push(currentGroup);
    }
    currentGroup.messages.push(message);
  }

  return groups;
}

type MessageBubbleProps = Readonly<{
  message: TicketMessage;
}>;

function MessageBubble({ message }: MessageBubbleProps) {
  const isCustomer = message.authorRole === "CUSTOMER";
  const isAI = message.messageKind === "AI_RESPONSE" || message.authorRole === "SYSTEM";
  const isSystem = message.messageKind === "SYSTEM_NOTE";

  if (isSystem) {
    return (
      <div className="flex items-center justify-center py-2">
        <div className="flex items-center gap-2 rounded-full bg-muted px-4 py-1.5 text-xs text-muted-foreground">
          <AlertCircle className="h-3 w-3" />
          <span>{message.content}</span>
          <span className="text-muted-foreground/60">{formatMessageTime(message.createdAt)}</span>
        </div>
      </div>
    );
  }

  if (isAI) {
    return (
      <div className="flex justify-start">
        <div className="max-w-[85%] md:max-w-[70%]">
          <div className="rounded-2xl rounded-tl-md bg-gradient-to-r from-purple-50 to-violet-50 px-4 py-2.5 shadow-sm border border-purple-100">
            <div className="mb-1 flex items-center gap-1.5">
              <Sparkles className="h-3 w-3 text-purple-500" />
              <span className="text-xs font-medium text-purple-700">AI Assistant</span>
            </div>
            <p className="whitespace-pre-wrap text-sm text-foreground">{message.content}</p>
            <p className="mt-1 text-right text-[10px] text-muted-foreground">
              {formatMessageTime(message.createdAt)}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (isCustomer) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] md:max-w-[70%]">
          <div className="rounded-2xl rounded-tr-md bg-sky-100 px-4 py-2.5 shadow-sm">
            <p className="whitespace-pre-wrap text-sm text-sky-900">{message.content}</p>
            <p className="mt-1 text-right text-[10px] text-sky-600">
              {formatMessageTime(message.createdAt)}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-start">
      <div className="max-w-[85%] md:max-w-[70%]">
        <div className="rounded-2xl rounded-tl-md bg-muted px-4 py-2.5 shadow-sm">
          <div className="mb-1 flex items-center gap-1.5">
            <span className="text-xs font-medium text-primary">{message.authorName}</span>
            <Badge variant="outline" className="h-4 border-0 bg-primary/10 px-1.5 text-[10px] text-primary">
              Agent
            </Badge>
          </div>
          <p className="whitespace-pre-wrap text-sm text-foreground">{message.content}</p>
          <p className="mt-1 text-right text-[10px] text-muted-foreground">
            {formatMessageTime(message.createdAt)}
          </p>
        </div>
      </div>
    </div>
  );
}

type ConversationPanelProps = Readonly<{
  messages: TicketMessage[];
}>;

function ConversationPanel({ messages }: ConversationPanelProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const messageGroups = groupMessagesByDate(messages);

  if (messages.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
          <MessageSquare className="h-8 w-8 text-muted-foreground/60" />
        </div>
        <p className="mt-4 font-medium text-foreground">No messages yet</p>
        <p className="mt-1 text-sm text-muted-foreground">Be the first to start the conversation</p>
      </div>
    );
  }

  return (
    <div className="scrollbar-thin max-h-[calc(100vh-400px)] min-h-[300px] overflow-y-auto space-y-1 px-1">
      {messageGroups.map((group, groupIndex) => (
        <div key={groupIndex}>
          <DateSeparator date={group.date} />
          <div className="space-y-2">
            {group.messages.map((message) => (
              <MessageBubble key={message.id} message={message} />
            ))}
          </div>
        </div>
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
}

type DetailSectionProps = Readonly<{
  title: string;
  children: React.ReactNode;
}>;

function DetailSection({ title, children }: DetailSectionProps) {
  return (
    <div className="space-y-2">
      <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{title}</h4>
      {children}
    </div>
  );
}

type StatusBadgeProps = Readonly<{
  status: string;
  statusLabel: string;
}>;

function StatusBadge({ status, statusLabel }: StatusBadgeProps) {
  const config: Record<string, { icon: typeof CheckCircle }> = {
    RECEIVED: { icon: Clock },
    TRIAGING: { icon: Clock },
    WAITING_CUSTOMER: { icon: Clock },
    ASSIGNED: { icon: User },
    IN_PROGRESS: { icon: Clock },
    RESOLVED: { icon: CheckCircle },
    ESCALATED: { icon: AlertCircle },
    CLOSED: { icon: CheckCircle },
    AUTO_CLOSED_PENDING: { icon: Clock },
  };

  const { icon: Icon } = config[status] || { icon: Clock };

  return (
    <div className="flex items-center gap-2">
      <Icon className={cn("h-4 w-4", getTicketStatusIconClass(status))} />
      <Badge variant={getStatusTone(status)}>{statusLabel}</Badge>
    </div>
  );
}

export default function TicketDetailPage() {
  const data = useLoaderData<TicketDetailLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();
  const [reply, setReply] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [statusSubmitting, setStatusSubmitting] = useState(false);
  const [newStatus, setNewStatus] = useState<string>(data.status);
  const [statusReason, setStatusReason] = useState("");

  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, 30000);
    return () => clearInterval(interval);
  }, [revalidator]);

  useEffect(() => {
    setNewStatus(data.status);
    setStatusReason("");
  }, [data.status]);

  const onReply = async (event: FormEvent) => {
    event.preventDefault();
    if (!reply.trim() || submitting || !data.permissions.canReply) return;

    setSubmitting(true);
    try {
      await api.post(`/tickets/${data.id}/replies`, { content: reply });
      setReply("");
      await revalidator.revalidate();
    } catch (error) {
      toast.error("Failed to send reply");
      console.error("Failed to send reply:", error);
    } finally {
      setSubmitting(false);
    }
  };

  const onStatusChange = async () => {
    if (statusSubmitting || newStatus === data.status) {
      return;
    }

    if (newStatus === "ESCALATED" && !statusReason.trim()) {
      toast.error("Escalation reason is required.");
      return;
    }

    setStatusSubmitting(true);
    try {
      await api.patch(`/tickets/${data.id}/status`, {
        newStatus,
        reason: statusReason.trim() || undefined,
      });
      await revalidator.revalidate();
      setStatusReason("");
      toast.success(`Ticket status changed to ${formatLabel(newStatus)}`);
    } catch (error) {
      toast.error("Failed to change ticket status");
      console.error("Failed to change ticket status:", error);
    } finally {
      setStatusSubmitting(false);
    }
  };

  const getReplyButtonLabel = () => {
    if (submitting) {
      return "Sending...";
    }

    return "Send Reply";
  };

  const categoryLabel = data.category ? formatLabel(data.category) : "-";
  const resolvedCategoryLabel = data.categoryLabel || categoryLabel;
  const queueLabel = data.queue ? formatLabel(data.queue) : "-";
  const resolvedQueueLabel = data.queueLabel || queueLabel;
  const priorityLabel = data.priorityLabel || formatLabel(data.priority);
  const statusLabel = data.statusLabel || formatLabel(data.status);
  const canChangeStatus = data.permissions.canChangeStatus;
  const statusOptions = data.permissions.allowedStatusTransitions;

  const onAssignSelf = async () => {
    try {
      await api.patch(`/tickets/${data.id}/assign-self`);
      await revalidator.revalidate();
      toast.success("Ticket assigned to you");
    } catch (error) {
      toast.error("Failed to assign ticket");
      console.error("Failed to assign ticket:", error);
    }
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
            <StatusBadge status={data.status} statusLabel={statusLabel} />
            {data.escalated && (
              <Badge variant="destructive" className="gap-1">
                <AlertCircle className="h-3 w-3" />
                Escalated
              </Badge>
            )}
          </div>
          <h1 className="mt-1 truncate text-xl font-bold text-foreground">{data.subject}</h1>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <MessageSquare className="h-4 w-4 text-primary" />
                Conversation
                <span className="ml-auto text-xs font-normal text-muted-foreground">
                  {data.messages.length} messages
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ConversationPanel messages={data.messages} />

              <form onSubmit={onReply} className="space-y-3">
                <Textarea
                  placeholder="Type your reply..."
                  value={reply}
                  onChange={(e) => setReply(e.target.value)}
                  rows={3}
                  className="resize-none"
                  disabled={!data.permissions.canReply}
                />
                <div className="flex justify-end">
                  <Button type="submit" disabled={!reply.trim() || submitting || !data.permissions.canReply}>
                    <Send className="h-4 w-4 mr-2" />
                    {getReplyButtonLabel()}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {data.permissions.canAssignSelf && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Work Assignment</CardTitle>
              </CardHeader>
              <CardContent>
                <Button type="button" onClick={() => void onAssignSelf()}>
                  Assign To Me
                </Button>
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Ticket Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <DetailSection title="Category">
                <p className="text-sm">{resolvedCategoryLabel}</p>
              </DetailSection>
              <DetailSection title="Priority">
                <Badge variant={getPriorityTone(data.priority)}>{priorityLabel}</Badge>
              </DetailSection>
              <DetailSection title="Queue">
                <p className="text-sm">{resolvedQueueLabel}</p>
              </DetailSection>
              <Separator />
              <DetailSection title="Created">
                <p className="text-sm">{formatDateTime(data.createdAt)}</p>
              </DetailSection>
              <DetailSection title="Last Activity">
                <p className="text-sm">{formatDateTime(data.lastActivityAt)}</p>
              </DetailSection>
            </CardContent>
          </Card>

          {canChangeStatus && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Status Control</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Select value={newStatus} onValueChange={setNewStatus}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={data.status}>{formatLabel(data.status)} (Current)</SelectItem>
                    {statusOptions.map((status) => (
                      <SelectItem key={status} value={status}>
                        {formatLabel(status)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Textarea
                  value={statusReason}
                  onChange={(event) => setStatusReason(event.target.value)}
                  rows={3}
                  placeholder={
                    newStatus === "ESCALATED"
                      ? "Reason for escalation (required)"
                      : "Reason for status update (optional)"
                  }
                />
                <Button
                  type="button"
                  onClick={() => void onStatusChange()}
                  disabled={
                    statusSubmitting
                    || newStatus === data.status
                    || (newStatus === "ESCALATED" && !statusReason.trim())
                  }
                  className="w-full"
                >
                  {statusSubmitting ? "Updating..." : "Update Status"}
                </Button>
              </CardContent>
            </Card>
          )}

          {data.customer && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Customer</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-primary/10 text-primary">
                      {getInitials(data.customer.fullName || data.customer.username)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium">{data.customer.fullName || data.customer.username}</p>
                    <p className="text-sm text-muted-foreground">{data.customer.email}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {data.assignedAgent && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Assigned Agent</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-green-100 text-green-700">
                      {getInitials(data.assignedAgent.fullName || data.assignedAgent.username)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium">{data.assignedAgent.fullName || data.assignedAgent.username}</p>
                    <p className="text-sm text-muted-foreground">{data.assignedAgent.email}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
