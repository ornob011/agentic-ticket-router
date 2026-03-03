import type { UserRole } from "@/lib/api";

type RoleBadgeVariant = "default" | "destructive" | "warning" | "success" | "info" | "secondary";

const AGENT_WORKSPACE_ROLES: UserRole[] = ["AGENT", "SUPERVISOR", "ADMIN"];
const SUPERVISOR_WORKSPACE_ROLES: UserRole[] = ["SUPERVISOR", "ADMIN"];

export function getRoleBadgeVariant(role: string | null | undefined): RoleBadgeVariant {
  if (role === "ADMIN") {
    return "destructive";
  }

  if (role === "SUPERVISOR") {
    return "warning";
  }

  if (role === "AGENT") {
    return "default";
  }

  if (role === "CUSTOMER") {
    return "info";
  }

  return "secondary";
}

export function canAccessAgentWorkspace(role: UserRole | null | undefined): boolean {
  if (!role) {
    return false;
  }

  return AGENT_WORKSPACE_ROLES.includes(role);
}

export function canAccessSupervisorWorkspace(role: UserRole | null | undefined): boolean {
  if (!role) {
    return false;
  }

  return SUPERVISOR_WORKSPACE_ROLES.includes(role);
}

export function isCustomerRole(role: UserRole | null | undefined): boolean {
  return role === "CUSTOMER";
}
