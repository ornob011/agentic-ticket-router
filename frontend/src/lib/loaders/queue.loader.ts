import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";

export type QueueLoaderData = PagedResponse<TicketSummary>;

export async function queueLoader({ params }: LoaderFunctionArgs): Promise<QueueLoaderData> {
  return apiGet<PagedResponse<TicketSummary>>(`/tickets?scope=queue&queue=${params.queue}&page=0&size=50`);
}
