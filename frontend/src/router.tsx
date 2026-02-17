import { lazy, Suspense } from "react";
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  useRouteLoaderData,
} from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import RouteErrorBoundary from "@/components/RouteErrorBoundary";
import LoginPage from "@/pages/LoginPage";
import SignupPage from "@/pages/SignupPage";
import { Skeleton } from "@/components/ui";
import { appRoutes } from "@/lib/routes";

import {
  rootLoader,
  type RootLoaderData,
  dashboardLoader,
  type DashboardLoaderData,
  ticketsLoader,
  type TicketsLoaderData,
  ticketDetailLoader,
  type TicketDetailLoaderData,
  queueLoader,
  type QueueLoaderData,
  reviewQueueLoader,
  type ReviewQueueLoaderData,
  escalationsLoader,
  type EscalationsLoaderData,
  escalationDetailLoader,
  type EscalationDetailLoaderData,
  adminModelsLoader,
  type AdminModelsLoaderData,
  adminPoliciesLoader,
  type AdminPoliciesLoaderData,
  adminUsersLoader,
  type AdminUsersLoaderData,
  auditLogLoader,
  type AuditLogLoaderData,
  settingsLoader,
  type SettingsLoaderData,
} from "@/lib/loaders";

const DashboardPage = lazy(() => import("@/pages/DashboardPage"));
const TicketsPage = lazy(() => import("@/pages/TicketsPage"));
const NewTicketPage = lazy(() => import("@/pages/NewTicketPage"));
const TicketDetailPage = lazy(() => import("@/pages/TicketDetailPage"));
const QueuePage = lazy(() => import("@/pages/QueuePage"));
const ReviewQueuePage = lazy(() => import("@/pages/ReviewQueuePage"));
const EscalationsPage = lazy(() => import("@/pages/EscalationsPage"));
const EscalationDetailPage = lazy(() => import("@/pages/EscalationDetailPage"));
const AdminModelsPage = lazy(() => import("@/pages/AdminModelsPage"));
const AdminPoliciesPage = lazy(() => import("@/pages/AdminPoliciesPage"));
const AdminUsersPage = lazy(() => import("@/pages/AdminUsersPage"));
const AuditLogPage = lazy(() => import("@/pages/AuditLogPage"));
const AccountSettingsPage = lazy(() => import("@/pages/AccountSettingsPage"));
const NotFoundPage = lazy(() => import("@/pages/errors/NotFoundPage"));
const ForbiddenPage = lazy(() => import("@/pages/errors/ForbiddenPage"));
const ServerErrorPage = lazy(() => import("@/pages/errors/ServerErrorPage"));

function PageSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

function ErrorPageSkeleton() {
  return (
    <div className="flex-1 flex items-center justify-center">
      <Skeleton className="h-64 w-96" />
    </div>
  );
}

function ProtectedRoute() {
  const appData = useRouteLoaderData<RootLoaderData>("app");

  if (!appData?.isAuthenticated) {
    return <Navigate to={appRoutes.login} replace />;
  }

  return <Outlet />;
}

export const router = createBrowserRouter([
  {
    id: "root",
    element: <Outlet />,
    errorElement: (
      <ErrorBoundary>
        <RouteErrorBoundary />
      </ErrorBoundary>
    ),
    children: [
      {
        path: appRoutes.login,
        element: <LoginPage />,
      },
      {
        path: appRoutes.signup,
        element: <SignupPage />,
      },
      {
        path: appRoutes.root,
        id: "app",
        element: <ProtectedRoute />,
        loader: rootLoader,
        errorElement: <RouteErrorBoundary />,
        children: [
          {
            element: <AppLayout />,
            errorElement: (
              <AppLayout>
                <RouteErrorBoundary />
              </AppLayout>
            ),
            children: [
              {
                index: true,
                element: <Navigate to={appRoutes.dashboard} replace />,
              },
              {
                path: "dashboard",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <DashboardPage />
                  </Suspense>
                ),
                loader: dashboardLoader,
              },
              {
                path: "tickets",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <TicketsPage />
                  </Suspense>
                ),
                loader: ticketsLoader,
              },
              {
                path: "agent/tickets",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <TicketsPage />
                  </Suspense>
                ),
                loader: ticketsLoader,
              },
              {
                path: "tickets/new",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <NewTicketPage />
                  </Suspense>
                ),
              },
              {
                path: "tickets/:ticketId",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <TicketDetailPage />
                  </Suspense>
                ),
                loader: ticketDetailLoader,
              },
              {
                path: "agent/tickets/:ticketId",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <TicketDetailPage />
                  </Suspense>
                ),
                loader: ticketDetailLoader,
              },
              {
                path: "agent/queues/:queue",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <QueuePage />
                  </Suspense>
                ),
                loader: queueLoader,
              },
              {
                path: "agent/review-queue",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <ReviewQueuePage />
                  </Suspense>
                ),
                loader: reviewQueueLoader,
              },
              {
                path: "supervisor/escalations",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <EscalationsPage />
                  </Suspense>
                ),
                loader: escalationsLoader,
              },
              {
                path: "supervisor/escalations/:escalationId",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <EscalationDetailPage />
                  </Suspense>
                ),
                loader: escalationDetailLoader,
              },
              {
                path: "admin/model-registry",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <AdminModelsPage />
                  </Suspense>
                ),
                loader: adminModelsLoader,
              },
              {
                path: "admin/policy-config",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <AdminPoliciesPage />
                  </Suspense>
                ),
                loader: adminPoliciesLoader,
              },
              {
                path: "admin/users",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <AdminUsersPage />
                  </Suspense>
                ),
                loader: adminUsersLoader,
              },
              {
                path: "admin/audit-log",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <AuditLogPage />
                  </Suspense>
                ),
                loader: auditLogLoader,
              },
              {
                path: "settings",
                element: (
                  <Suspense fallback={<PageSkeleton />}>
                    <AccountSettingsPage />
                  </Suspense>
                ),
                loader: settingsLoader,
              },
              {
                path: "403",
                element: (
                  <Suspense fallback={<ErrorPageSkeleton />}>
                    <ForbiddenPage />
                  </Suspense>
                ),
              },
              {
                path: "500",
                element: (
                  <Suspense fallback={<ErrorPageSkeleton />}>
                    <ServerErrorPage />
                  </Suspense>
                ),
              },
              {
                path: "404",
                element: (
                  <Suspense fallback={<ErrorPageSkeleton />}>
                    <NotFoundPage />
                  </Suspense>
                ),
              },
              {
                path: "*",
                element: <Navigate to={appRoutes.errors.notFound} replace />,
              },
            ],
          },
        ],
      },
      {
        path: "*",
        element: <Navigate to={appRoutes.errors.notFound} replace />,
      },
    ],
  },
]);

export type {
  RootLoaderData,
  DashboardLoaderData,
  TicketsLoaderData,
  TicketDetailLoaderData,
  QueueLoaderData,
  ReviewQueueLoaderData,
  EscalationsLoaderData,
  EscalationDetailLoaderData,
  AdminModelsLoaderData,
  AdminPoliciesLoaderData,
  AdminUsersLoaderData,
  AuditLogLoaderData,
  SettingsLoaderData,
};
