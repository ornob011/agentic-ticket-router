import { useLoaderData, useNavigate, useRevalidator, useRouteLoaderData } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { DashboardLoaderData, RootLoaderData } from "@/router";
import { getTicketMetadata } from "@/app/tickets";
import { canAccessAgentWorkspace, canAccessSupervisorWorkspace } from "@/lib/role-policy";
import { usePeriodicRevalidation } from "@/lib/hooks";
import { appRoutes } from "@/lib/routes";
import { DashboardScreen } from "@/widgets/dashboard/dashboard-screen";

export default function DashboardPage() {
  const data = useLoaderData<DashboardLoaderData>();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const navigate = useNavigate();
  const revalidator = useRevalidator();

  usePeriodicRevalidation(revalidator);

  const role = data.user.role;
  const isCustomer = appData?.user?.role === "CUSTOMER";
  const isAgent = canAccessAgentWorkspace(role);
  const isSupervisor = canAccessSupervisorWorkspace(role);

  const { data: ticketMetadata } = useQuery({
    queryKey: ["ticket-metadata", "dashboard"],
    queryFn: getTicketMetadata,
    enabled: isAgent,
    staleTime: 60_000,
  });

  const agentQueueTarget = ticketMetadata?.accessibleQueues[0]?.code
    ? appRoutes.agent.queues.byCode(ticketMetadata.accessibleQueues[0].code)
    : null;

  return (
    <DashboardScreen
      data={data}
      isCustomer={isCustomer}
      isAgent={isAgent}
      isSupervisor={isSupervisor}
      agentQueueTarget={agentQueueTarget}
      onNavigate={(target) => void navigate(target)}
    />
  );
}
