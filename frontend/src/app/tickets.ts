import { apiGet } from "@/lib/api-loader";
import type { AssignableAgentOption, TicketMetadataResponse } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export async function getTicketMetadata(): Promise<TicketMetadataResponse> {
  return apiGet<TicketMetadataResponse>(endpoints.tickets.metadata);
}

export async function getAssignableAgents(): Promise<AssignableAgentOption[]> {
  return apiGet<AssignableAgentOption[]>(endpoints.tickets.assignableAgents);
}
