import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import type { ComponentType, LazyExoticComponent } from "react";
import RequireAuth from "./components/RequireAuth";
import { AppLayout } from "./components/layout";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import { Skeleton } from "@/components/ui";

const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const TicketsPage = lazy(() => import("./pages/TicketsPage"));
const NewTicketPage = lazy(() => import("./pages/NewTicketPage"));
const TicketDetailPage = lazy(() => import("./pages/TicketDetailPage"));
const QueuePage = lazy(() => import("./pages/QueuePage"));
const ReviewQueuePage = lazy(() => import("./pages/ReviewQueuePage"));
const EscalationsPage = lazy(() => import("./pages/EscalationsPage"));
const EscalationDetailPage = lazy(() => import("./pages/EscalationDetailPage"));
const AdminModelsPage = lazy(() => import("./pages/AdminModelsPage"));
const AdminPoliciesPage = lazy(() => import("./pages/AdminPoliciesPage"));
const AdminUsersPage = lazy(() => import("./pages/AdminUsersPage"));
const AuditLogPage = lazy(() => import("./pages/AuditLogPage"));

type LazyRoute = {
  path: string;
  component: LazyExoticComponent<ComponentType>;
};

const protectedRoutes: LazyRoute[] = [
  { path: "/app/dashboard", component: DashboardPage },
  { path: "/app/tickets", component: TicketsPage },
  { path: "/app/tickets/new", component: NewTicketPage },
  { path: "/app/tickets/:ticketId", component: TicketDetailPage },
  { path: "/app/agent/tickets/:ticketId", component: TicketDetailPage },
  { path: "/app/agent/queues/:queue", component: QueuePage },
  { path: "/app/agent/review-queue", component: ReviewQueuePage },
  { path: "/app/supervisor/escalations", component: EscalationsPage },
  { path: "/app/supervisor/escalations/:escalationId", component: EscalationDetailPage },
  { path: "/app/admin/model-registry", component: AdminModelsPage },
  { path: "/app/admin/policy-config", component: AdminPoliciesPage },
  { path: "/app/admin/users", component: AdminUsersPage },
  { path: "/app/admin/audit-log", component: AuditLogPage },
];

function PageSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/app/login" element={<LoginPage />} />
      <Route path="/app/signup" element={<SignupPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          {protectedRoutes.map((route) => {
            const PageComponent = route.component;

            return (
              <Route
                key={route.path}
                path={route.path}
                element={
                  <Suspense fallback={<PageSkeleton />}>
                    <PageComponent />
                  </Suspense>
                }
              />
            );
          })}
        </Route>
      </Route>
      <Route path="/app/*" element={<Suspense fallback={null}><DashboardPage /></Suspense>} />
      <Route path="*" element={<Suspense fallback={null}><DashboardPage /></Suspense>} />
    </Routes>
  );
}
