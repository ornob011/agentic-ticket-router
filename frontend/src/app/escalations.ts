import { apiGet } from "@/lib/api-loader";
import type { AssignableSupervisorOption } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export async function getAssignableSupervisors(): Promise<AssignableSupervisorOption[]> {
  return apiGet<AssignableSupervisorOption[]>(endpoints.supervisor.assignableSupervisors);
}
