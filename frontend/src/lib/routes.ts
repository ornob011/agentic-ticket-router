export const appRoutes = {
  root: "/app",
  login: "/app/login",
  signup: "/app/signup",
  dashboard: "/app/dashboard",
  settings: "/app/settings",
  errors: {
    forbidden: "/app/403",
    server: "/app/500",
    notFound: "/app/404",
  },
  tickets: {
    list: "/app/tickets",
    create: "/app/tickets/new",
    detail: (ticketId: string | number) => `/app/tickets/${ticketId}`,
    byStatus: (status: string) => `/app/tickets?status=${status}`,
  },
  agent: {
    ticketDetail: (ticketId: string | number) => `/app/agent/tickets/${ticketId}`,
    queues: {
      all: "/app/agent/queues/ALL",
      byCode: (queueCode: string) => `/app/agent/queues/${queueCode}`,
      general: "/app/agent/queues/GENERAL_Q",
    },
    reviewQueue: "/app/agent/review-queue",
  },
  supervisor: {
    escalations: "/app/supervisor/escalations",
    escalationDetail: (escalationId: string | number) => `/app/supervisor/escalations/${escalationId}`,
  },
  admin: {
    modelRegistry: "/app/admin/model-registry",
    policyConfig: "/app/admin/policy-config",
    users: "/app/admin/users",
    auditLog: "/app/admin/audit-log",
  },
} as const;
