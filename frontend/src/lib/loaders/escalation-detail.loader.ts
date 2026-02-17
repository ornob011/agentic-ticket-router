import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { EscalationDetail } from "@/lib/api";
import type { LoaderFunctionArgs } from "react-router-dom";

export type EscalationDetailLoaderData = EscalationDetail;

export async function escalationDetailLoader({ params }: LoaderFunctionArgs): Promise<EscalationDetailLoaderData> {
  return apiGet<EscalationDetail>(endpoints.supervisor.escalationDetail(params.escalationId ?? ""));
}
