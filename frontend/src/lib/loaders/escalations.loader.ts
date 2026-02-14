import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, EscalationSummary } from "@/lib/api";

export type EscalationsLoaderData = PagedResponse<EscalationSummary>;

export async function escalationsLoader(): Promise<EscalationsLoaderData> {
  return apiGet<PagedResponse<EscalationSummary>>("/supervisor/escalations?page=0&size=50");
}
