import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";

export type TicketsLoaderData = PagedResponse<TicketSummary>;

export async function ticketsLoader(): Promise<TicketsLoaderData> {
  return apiGet<PagedResponse<TicketSummary>>("/tickets?scope=mine&page=0&size=20");
}
