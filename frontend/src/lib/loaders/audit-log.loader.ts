import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, AuditEventItem } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export type AuditLogLoaderData = PagedResponse<AuditEventItem>;

export async function auditLogLoader(): Promise<AuditLogLoaderData> {
  return apiGet<PagedResponse<AuditEventItem>>(endpoints.admin.auditLog);
}
