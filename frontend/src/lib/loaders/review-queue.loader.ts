import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";

export type ReviewQueueLoaderData = PagedResponse<TicketSummary>;

export async function reviewQueueLoader(): Promise<ReviewQueueLoaderData> {
  return apiGet<PagedResponse<TicketSummary>>("/tickets?scope=review&page=0&size=50");
}
