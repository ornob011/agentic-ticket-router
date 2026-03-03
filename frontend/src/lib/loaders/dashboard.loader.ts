import { apiGet } from "@/lib/api-loader";
import type { DashboardResponse } from "@/lib/api";

export type DashboardLoaderData = DashboardResponse;

export async function dashboardLoader(): Promise<DashboardLoaderData> {
  return apiGet<DashboardResponse>("/dashboard");
}
