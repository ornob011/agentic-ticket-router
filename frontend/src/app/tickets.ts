import { apiGet } from "@/lib/api-loader";
import type { TicketMetadataResponse } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export async function getTicketMetadata(): Promise<TicketMetadataResponse> {
  return apiGet<TicketMetadataResponse>(endpoints.tickets.metadata);
}
