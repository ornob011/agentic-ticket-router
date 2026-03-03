import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { PolicyInfo } from "@/lib/api";

export type AdminPoliciesLoaderData = PolicyInfo[];

export async function adminPoliciesLoader(): Promise<AdminPoliciesLoaderData> {
  return apiGet<PolicyInfo[]>(endpoints.admin.policyConfig);
}
