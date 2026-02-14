import axios from "axios";

export const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});

export type UserRole = "ADMIN" | "SUPERVISOR" | "AGENT" | "CUSTOMER";

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type UserMe = {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  roleLabel: string | null;
};

export type TicketSummary = {
  id: number;
  formattedTicketNo: string;
  subject: string;
  status: string;
  statusLabel: string | null;
  category: string | null;
  categoryLabel: string | null;
  priority: string | null;
  priorityLabel: string | null;
  queue: string | null;
  queueLabel: string | null;
  lastActivityAt: string;
  customerName: string | null;
  assignedAgentName: string | null;
};

export type TicketMessage = {
  id: number;
  content: string;
  authorName: string;
  authorRole: string;
  messageKind: string;
  visibleToCustomer: boolean;
  createdAt: string;
};

export type TicketRoutingItem = {
  id: number;
  version: number;
  category: string;
  categoryLabel: string | null;
  priority: string;
  priorityLabel: string | null;
  queue: string;
  queueLabel: string | null;
  nextAction: string;
  nextActionLabel: string | null;
  confidence: number;
  overridden: boolean;
  overrideReason: string | null;
  createdAt: string;
};

export type AuditEventItem = {
  id: number;
  eventType: string;
  eventTypeLabel: string | null;
  description: string;
  performedBy: string;
  createdAt: string;
};

export type TicketDetail = {
  id: number;
  formattedTicketNo: string;
  subject: string;
  status: string;
  statusLabel: string | null;
  category: string | null;
  categoryLabel: string | null;
  priority: string;
  priorityLabel: string | null;
  queue: string | null;
  queueLabel: string | null;
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
  reopenCount: number;
  escalated: boolean;
  customer: UserMe | null;
  assignedAgent: UserMe | null;
  messages: TicketMessage[];
  auditEvents: AuditEventItem[];
  routingHistory: TicketRoutingItem[];
};

export type DashboardResponse = {
  user: UserMe;
  customer: {
    openTickets: number;
    waitingOnMe: number;
    resolvedTickets: number;
    closedTickets: number;
  } | null;
  agent: {
    myAssignedCount: number;
    queueBilling: number;
    queueTech: number;
    queueOps: number;
    queueSecurity: number;
    queueAccount: number;
    queueGeneral: number;
  } | null;
  supervisor: {
    pendingEscalations: number;
    slaBreaches: number;
    humanReviewCount: number;
  } | null;
  admin: {
    totalUsers: number;
    totalTickets: number;
    activeModelTag: string;
    routingSuccessRate: number;
    avgRoutingLatency: number | null;
  } | null;
  recentTickets: TicketSummary[];
};

export type EscalationSummary = {
  id: number;
  ticketId: number;
  formattedTicketNo: string;
  reason: string;
  resolved: boolean;
  createdAt: string;
  assignedSupervisor: string | null;
};

export type EscalationDetail = {
  id: number;
  ticketId: number;
  formattedTicketNo: string;
  reason: string;
  resolved: boolean;
  resolutionNotes: string | null;
  createdAt: string;
  resolvedAt: string | null;
  assignedSupervisor: string | null;
  resolvedBy: string | null;
};

export type UserInfo = {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
  roleLabel: string | null;
  active: boolean;
};

export type ModelInfo = {
  id: number;
  modelTag: string;
  provider: string;
  active: boolean;
  activatedBy: number | null;
  activatedAt: string | null;
};

export type PolicyInfo = {
  id: number;
  configKey: string;
  configValue: number;
  active: boolean;
};

export type QueueStats = {
  assignedCount: number;
  inProgressCount: number;
  resolvedCount: number;
  escalatedCount: number;
  awaitingCustomerCount: number;
  triagingCount: number;
};

export type NotificationDto = {
  id: number;
  title: string;
  body: string;
  type: string;
  ticketId: number | null;
  link: string | null;
  read: boolean;
  createdAt: string;
};
