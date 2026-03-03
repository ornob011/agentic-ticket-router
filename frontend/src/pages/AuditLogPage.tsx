import { useLoaderData } from "react-router-dom";
import type { AuditLogLoaderData } from "@/router";
import { AuditLogScreen } from "@/widgets/audit-log/audit-log-screen";

export default function AuditLogPage() {
  const data = useLoaderData<AuditLogLoaderData>();

  return (
    <AuditLogScreen
      events={data?.content ?? []}
      totalElements={data?.totalElements ?? 0}
    />
  );
}
