import type { ReactNode } from "react";
import { useNavigate, useRouteLoaderData } from "react-router-dom";
import { ArrowLeft, Home, Plus, RefreshCw, Ticket } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { StatusDot } from "@/components/ui/status-dot";
import { appRoutes } from "@/lib/routes";
import type { RootLoaderData } from "@/router";

type AppErrorContentProps = {
  icon: ReactNode;
  title: string;
  description: string;
  code: string;
  detail: string;
  apiErrorCode?: string;
  showRetry?: boolean;
};

export function AppErrorContent({
  icon,
  title,
  description,
  code,
  detail,
  apiErrorCode,
  showRetry = false,
}: Readonly<AppErrorContentProps>) {
  const navigate = useNavigate();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const isCustomer = appData?.user?.role === "CUSTOMER";

  return (
    <div className="space-y-6">
      <PageHeader title={title} description={description}>
        <Button variant="outline" onClick={() => void navigate(appRoutes.tickets.list)}>
          <Ticket className="mr-2 h-4 w-4" />
          View Tickets
        </Button>
      </PageHeader>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="p-8">
            <div className="flex flex-col items-center text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
                {icon}
              </div>
              <span className="mt-4 rounded-md bg-muted px-3 py-1 font-mono text-xs text-muted-foreground">
                Error {code}
              </span>
              <p className="mt-4 max-w-xl text-sm text-muted-foreground">{detail}</p>
              {apiErrorCode && (
                <p className="mt-3 font-mono text-xs text-muted-foreground/70">
                  Reference: {apiErrorCode}
                </p>
              )}
              <div className="mt-6 flex flex-wrap justify-center gap-3">
                <Button variant="outline" onClick={() => navigate(-1)}>
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Go Back
                </Button>
                <Button onClick={() => void navigate(appRoutes.dashboard)}>
                  <Home className="mr-2 h-4 w-4" />
                  Dashboard
                </Button>
                {showRetry && (
                  <Button variant="secondary" onClick={() => window.location.reload()}>
                    <RefreshCw className="mr-2 h-4 w-4" />
                    Retry
                  </Button>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <StatusDot status="online" pulse size="sm" />
              Quick Actions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {isCustomer && (
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={() => void navigate(appRoutes.tickets.create)}
              >
                <Plus className="mr-2 h-4 w-4" />
                Create New Ticket
              </Button>
            )}
            <Button
              variant="outline"
              className="w-full justify-start"
              onClick={() => void navigate(appRoutes.tickets.list)}
            >
              <Ticket className="mr-2 h-4 w-4" />
              View All Tickets
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
