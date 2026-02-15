import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";

export type QueueLoaderData = PagedResponse<TicketSummary>;

export async function queueLoader({ params }: LoaderFunctionArgs): Promise<QueueLoaderData> {
  const queue = params.queue;
  if (queue === "ALL") {
    return apiGet<PagedResponse<TicketSummary>>("/tickets?scope=queue&page=0&size=50");
  }

  return apiGet<PagedResponse<TicketSummary>>(`/tickets?scope=queue&queue=${queue}&page=0&size=50`);
}
