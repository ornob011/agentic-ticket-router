import { apiGet } from "@/lib/api-loader";
import type { PagedResponse, AuditEventItem } from "@/lib/api";

export type AuditLogLoaderData = PagedResponse<AuditEventItem>;

export async function auditLogLoader(): Promise<AuditLogLoaderData> {
  return apiGet<PagedResponse<AuditEventItem>>("/admin/audit-log?page=0&size=50");
}
