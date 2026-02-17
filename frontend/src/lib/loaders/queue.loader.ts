import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";
import { endpoints } from "@/lib/endpoints";

export type QueueLoaderData = PagedResponse<TicketSummary>;

export async function queueLoader({ params }: LoaderFunctionArgs): Promise<QueueLoaderData> {
  const queue = params.queue;
  return apiGet<PagedResponse<TicketSummary>>(endpoints.tickets.listQueue(queue));
}
