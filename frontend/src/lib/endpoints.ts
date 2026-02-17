export const endpoints = {
  auth: {
    me: "/auth/me",
    login: "/auth/login",
    logout: "/auth/logout",
    profile: "/auth/profile",
    settings: "/auth/settings",
    changePassword: "/auth/change-password",
    signup: "/auth/signup",
    signupOptions: "/auth/signup-options",
  },
  tickets: {
    listMine: (status?: string | null) => {
      const statusQuery = status ? `&status=${status}` : "";
      return `/tickets?scope=mine&page=0&size=20${statusQuery}`;
    },
    listQueue: (queue?: string | null) => {
      if (!queue || queue === "ALL") {
        return "/tickets?scope=queue&page=0&size=50";
      }
      return `/tickets?scope=queue&queue=${queue}&page=0&size=50`;
    },
    listReview: "/tickets?scope=review&page=0&size=50",
    create: "/tickets",
    detail: (ticketId: string | number) => `/tickets/${ticketId}`,
    replies: (ticketId: string | number) => `/tickets/${ticketId}/replies`,
    status: (ticketId: string | number) => `/tickets/${ticketId}/status`,
    assignSelf: (ticketId: string | number) => `/tickets/${ticketId}/assign-self`,
    metadata: "/tickets/meta",
  },
  supervisor: {
    escalations: "/supervisor/escalations?page=0&size=50",
    escalationDetail: (escalationId: string | number) => `/supervisor/escalations/${escalationId}`,
    resolveEscalation: (escalationId: string | number) => `/supervisor/escalations/${escalationId}/resolve`,
  },
  admin: {
    users: "/admin/users",
    queueMemberships: "/admin/queue-memberships",
    queueMembership: (membershipId: string | number) => `/admin/queue-memberships/${membershipId}`,
    policyConfig: "/admin/policy-config",
    policyStatus: "/admin/policy-config/status",
    resetPolicy: (configKey: string) => `/admin/policy-config/${configKey}/reset`,
    modelRegistry: "/admin/model-registry",
    activateModel: "/admin/model-registry/activate",
    auditLog: "/admin/audit-log?page=0&size=50",
  },
} as const;
