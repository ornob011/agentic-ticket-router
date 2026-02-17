import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, TicketSummary } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";

export type TicketsLoaderData = PagedResponse<TicketSummary>;

const VALID_TICKET_STATUS = new Set([
  "RECEIVED",
  "TRIAGING",
  "WAITING_CUSTOMER",
  "ASSIGNED",
  "IN_PROGRESS",
  "RESOLVED",
  "ESCALATED",
  "AUTO_CLOSED_PENDING",
  "CLOSED",
]);

export async function ticketsLoader({ request }: LoaderFunctionArgs): Promise<TicketsLoaderData> {
  const url = new URL(request.url);
  const statusParam = url.searchParams.get("status");
  const status = statusParam && VALID_TICKET_STATUS.has(statusParam) ? statusParam : null;
  const statusQuery = status ? `&status=${status}` : "";
  return apiGet<PagedResponse<TicketSummary>>(`/tickets?scope=mine&page=0&size=20${statusQuery}`);
}
