import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { PagedResponse, TicketSummary } from "@/lib/api";

export type ReviewQueueLoaderData = PagedResponse<TicketSummary>;

export async function reviewQueueLoader(): Promise<ReviewQueueLoaderData> {
  return apiGet<PagedResponse<TicketSummary>>(endpoints.tickets.listReview);
}
