import { apiGet } from "@/lib/api-loader";
import type { PolicyInfo } from "@/lib/api";

export type AdminPoliciesLoaderData = PolicyInfo[];

export async function adminPoliciesLoader(): Promise<AdminPoliciesLoaderData> {
  return apiGet<PolicyInfo[]>("/admin/policy-config");
}
