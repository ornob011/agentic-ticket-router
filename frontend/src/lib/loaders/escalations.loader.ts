import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, EscalationSummary } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export type EscalationsLoaderData = PagedResponse<EscalationSummary>;

export async function escalationsLoader(): Promise<EscalationsLoaderData> {
  return apiGet<PagedResponse<EscalationSummary>>(endpoints.supervisor.escalations);
}
