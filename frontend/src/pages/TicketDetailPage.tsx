import { FormEvent, useRef, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type TicketDetail, type TicketMessage } from "@/lib/api";
import { formatLabel, getStatusTone, formatDateTime, getPriorityTone, cn } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DateSeparator } from "@/components/ui/date-separator";
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

function TicketDetailSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-64" />
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Skeleton className="h-96" />
        </div>
        <div className="space-y-4">
          <Skeleton className="h-48" />
          <Skeleton className="h-32" />
        </div>
      </div>
    </div>
  );
}

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
  const config: Record<string, { icon: typeof CheckCircle; color: string }> = {
    RECEIVED: { icon: Clock, color: "text-slate-500" },
    TRIAGING: { icon: Clock, color: "text-blue-500" },
    WAITING_CUSTOMER: { icon: Clock, color: "text-amber-500" },
    ASSIGNED: { icon: User, color: "text-indigo-500" },
    IN_PROGRESS: { icon: Clock, color: "text-sky-500" },
    RESOLVED: { icon: CheckCircle, color: "text-green-500" },
    ESCALATED: { icon: AlertCircle, color: "text-red-500" },
    CLOSED: { icon: CheckCircle, color: "text-slate-500" },
    AUTO_CLOSED_PENDING: { icon: Clock, color: "text-slate-500" },
  };

  const { icon: Icon, color } = config[status] || { icon: Clock, color: "text-slate-500" };

  return (
    <div className="flex items-center gap-2">
      <Icon className={cn("h-4 w-4", color)} />
      <Badge variant={getStatusTone(status)}>{statusLabel}</Badge>
    </div>
  );
}

export default function TicketDetailPage() {
  const { ticketId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [reply, setReply] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["ticket", ticketId],
    queryFn: async () => (await api.get<TicketDetail>(`/tickets/${ticketId}`)).data,
    enabled: Boolean(ticketId),
    refetchInterval: 30000,
  });

  const onReply = async (event: FormEvent) => {
    event.preventDefault();
    if (!ticketId || !reply.trim() || submitting) return;

    setSubmitting(true);
    try {
      await api.post(`/tickets/${ticketId}/replies`, { content: reply });
      setReply("");
      await queryClient.invalidateQueries({ queryKey: ["ticket", ticketId] });
    } catch (error) {
      console.error("Failed to send reply:", error);
    } finally {
      setSubmitting(false);
    }
  };

  if (isLoading || !data) {
    return <TicketDetailSkeleton />;
  }

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
            <CardContent>
              <ConversationPanel messages={data.messages} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Reply</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={onReply} className="space-y-4">
                <Textarea
                  placeholder="Type your reply..."
                  value={reply}
                  onChange={(e) => setReply(e.target.value)}
                  rows={4}
                  className="resize-none"
                />
                <div className="flex items-center justify-between">
                  <p className="text-xs text-muted-foreground">
                    Press <kbd className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs">Enter</kbd> to
                    send, <kbd className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs">Shift+Enter</kbd>{" "}
                    for new line
                  </p>
                  <Button type="submit" disabled={!reply.trim() || submitting}>
                    <Send className="mr-2 h-4 w-4" />
                    {getReplyButtonLabel()}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Ticket Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <DetailSection title="Priority">
                <Badge variant={getPriorityTone(data.priority)} className="text-sm">
                  {priorityLabel}
                </Badge>
              </DetailSection>

              <DetailSection title="Category">
                <span className="text-sm">{resolvedCategoryLabel}</span>
              </DetailSection>

              <DetailSection title="Queue">
                <span className="text-sm">{resolvedQueueLabel}</span>
              </DetailSection>

              <Separator />

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs text-muted-foreground">Created</p>
                  <p className="mt-0.5 font-medium">{formatDateTime(data.createdAt)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Updated</p>
                  <p className="mt-0.5 font-medium">{formatDateTime(data.updatedAt)}</p>
                </div>
              </div>

              {data.reopenCount > 0 && (
                <div className="rounded-lg bg-amber-50 p-2 text-xs text-amber-700">
                  This ticket has been reopened {data.reopenCount} time(s)
                </div>
              )}
            </CardContent>
          </Card>

          {data.customer && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base">
                  <User className="h-4 w-4" />
                  Customer
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-sky-100 text-sky-700">
                      {getInitials(data.customer.fullName || data.customer.username)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium">{data.customer.fullName || data.customer.username}</p>
                    <p className="text-xs text-muted-foreground">@{data.customer.username}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {data.assignedAgent && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base">
                  <User className="h-4 w-4" />
                  Assigned Agent
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-primary text-primary-foreground">
                      {getInitials(data.assignedAgent.fullName || data.assignedAgent.username)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium">{data.assignedAgent.fullName || data.assignedAgent.username}</p>
                    <p className="text-xs text-muted-foreground">Support Agent</p>
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
