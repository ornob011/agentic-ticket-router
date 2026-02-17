import { useLoaderData, useNavigate, useRevalidator, useRouteLoaderData, useSearchParams } from "react-router-dom";
import type { RootLoaderData, TicketsLoaderData } from "@/router";
import { appRoutes } from "@/lib/routes";
import { formatLabel } from "@/lib/utils";
import { usePeriodicRevalidation } from "@/lib/hooks";
import { TicketWorklist } from "@/components/ui/ticket-worklist";
import { Button } from "@/components/ui/button";
import { Plus, Ticket } from "lucide-react";

export default function TicketsPage() {
  const data = useLoaderData<TicketsLoaderData>();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const navigate = useNavigate();
  const revalidator = useRevalidator();
  const [searchParams] = useSearchParams();
  const isCustomer = appData?.user?.role === "CUSTOMER";
  const statusFilter = searchParams.get("status");
  const statusFilterLabel = formatLabel(statusFilter);
  const tickets = data?.content ?? [];

  usePeriodicRevalidation(revalidator);

  return (
    <TicketWorklist
      title="My Tickets"
      description={statusFilter ? `Filtered by status: ${statusFilterLabel}` : "Track and manage your support requests"}
      headerActions={
        isCustomer ? (
          <Button onClick={() => void navigate(appRoutes.tickets.create)}>
            <Plus className="mr-2 h-4 w-4" />
            New Ticket
          </Button>
        ) : undefined
      }
      emptyIcon={Ticket}
      emptyTitle="No tickets yet"
      emptyDescription={
        isCustomer
          ? "Create your first support ticket to get started with our AI-powered routing system."
          : "No tickets found in your current scope."
      }
      emptyFilteredTitle="No tickets yet"
      emptyFilteredDescription={
        isCustomer
          ? "Create your first support ticket to get started with our AI-powered routing system."
          : "No tickets found in your current scope."
      }
      emptyAction={
        isCustomer
          ? {
              label: "Create Ticket",
              icon: Plus,
              onClick: () => {
                void navigate(appRoutes.tickets.create);
              },
            }
          : undefined
      }
      summaryIcon={Ticket}
      summaryClassName="flex items-center gap-4 rounded-lg bg-muted/50 px-4 py-2 text-sm"
      summaryCountClassName="font-medium"
      summaryTextClassName="text-muted-foreground"
      summarySuffixLabel="total tickets"
      summaryActions={
        statusFilter ? (
          <Button variant="ghost" size="sm" onClick={() => void navigate(appRoutes.tickets.list)}>
            Clear Filter
          </Button>
        ) : undefined
      }
      showSummary={tickets.length > 0}
      tickets={tickets}
      totalElements={data?.totalElements ?? 0}
      queueOptions={[]}
      showFilters={false}
      navigatePathBuilder={(ticket) => appRoutes.tickets.detail(ticket.id)}
    />
  );
}
