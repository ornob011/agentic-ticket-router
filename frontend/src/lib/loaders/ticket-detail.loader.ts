import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { TicketDetail } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";

export type TicketDetailLoaderData = TicketDetail;

export async function ticketDetailLoader({ params }: LoaderFunctionArgs): Promise<TicketDetailLoaderData> {
  return apiGet<TicketDetail>(endpoints.tickets.detail(params.ticketId ?? ""));
}
