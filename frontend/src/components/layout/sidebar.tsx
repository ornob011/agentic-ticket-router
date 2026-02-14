import { Link, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  LayoutDashboard,
  Inbox,
  ClipboardCheck,
  AlertTriangle,
  Brain,
  Settings,
  Users,
  FileText,
  User,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import type { UserRole } from "@/lib/api";

interface SidebarProps {
  role?: UserRole;
  collapsed?: boolean;
  onToggle?: () => void;
}

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: UserRole[];
  activeMode: "exact" | "section" | "customerTickets";
}

const navItems: NavItem[] = [
  { to: "/app/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["CUSTOMER", "AGENT", "SUPERVISOR", "ADMIN"], activeMode: "exact" },
  { to: "/app/tickets", label: "My Tickets", icon: Inbox, roles: ["CUSTOMER"], activeMode: "customerTickets" },
  { to: "/app/tickets/new", label: "New Ticket", icon: ClipboardCheck, roles: ["CUSTOMER"], activeMode: "exact" },
  { to: "/app/agent/queues/GENERAL_Q", label: "Queue Inbox", icon: Inbox, roles: ["AGENT", "SUPERVISOR", "ADMIN"], activeMode: "section" },
  { to: "/app/agent/review-queue", label: "Review Queue", icon: ClipboardCheck, roles: ["SUPERVISOR", "ADMIN"], activeMode: "exact" },
  { to: "/app/supervisor/escalations", label: "Escalations", icon: AlertTriangle, roles: ["SUPERVISOR", "ADMIN"], activeMode: "exact" },
  { to: "/app/profile", label: "Profile", icon: User, roles: ["CUSTOMER", "AGENT", "SUPERVISOR", "ADMIN"], activeMode: "exact" },
  { to: "/app/settings", label: "Settings", icon: Settings, roles: ["CUSTOMER", "AGENT", "SUPERVISOR", "ADMIN"], activeMode: "exact" },
];

const adminItems: NavItem[] = [
  { to: "/app/admin/model-registry", label: "Model Registry", icon: Brain, roles: ["ADMIN"], activeMode: "exact" },
  { to: "/app/admin/policy-config", label: "Policy Config", icon: Settings, roles: ["ADMIN"], activeMode: "exact" },
  { to: "/app/admin/users", label: "Users", icon: Users, roles: ["ADMIN"], activeMode: "exact" },
  { to: "/app/admin/audit-log", label: "Audit Log", icon: FileText, roles: ["ADMIN"], activeMode: "exact" },
];

function isCustomerTicketPath(pathname: string): boolean {
  if (pathname === "/app/tickets") {
    return true;
  }

  if (pathname === "/app/tickets/new") {
    return false;
  }

  if (!pathname.startsWith("/app/tickets/")) {
    return false;
  }

  const suffix = pathname.slice("/app/tickets/".length);
  if (!suffix) {
    return false;
  }

  for (let i = 0; i < suffix.length; i += 1) {
    const code = suffix.charCodeAt(i);
    const isDigit = code >= 48 && code <= 57;
    if (!isDigit) {
      return false;
    }
  }

  return true;
}

function isItemActive(pathname: string, item: NavItem): boolean {
  switch (item.activeMode) {
    case "exact":
      return pathname === item.to;
    case "customerTickets":
      return isCustomerTicketPath(pathname);
    case "section":
      return pathname === item.to || pathname.startsWith(`${item.to}/`);
    default:
      return false;
  }
}

type NavItemLinkProps = Readonly<{
  item: NavItem;
  collapsed?: boolean;
}>;

function NavItemLink({ item, collapsed }: NavItemLinkProps) {
  const location = useLocation();
  const Icon = item.icon;
  const isActive = isItemActive(location.pathname, item);
  let toneClass = "text-muted-foreground hover:bg-accent hover:text-accent-foreground";

  if (isActive) {
    toneClass = "bg-primary text-primary-foreground";
  }

  return (
    <Link
      to={item.to}
      className={cn(
        "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
        toneClass,
        collapsed && "justify-center px-2"
      )}
    >
      <Icon className="h-5 w-5 shrink-0" />
      {!collapsed && <span>{item.label}</span>}
    </Link>
  );
}

export function Sidebar(
  { role, collapsed = false, onToggle }: Readonly<SidebarProps>
) {
  const filteredNavItems = navItems.filter((item) => role && item.roles.includes(role));
  const filteredAdminItems = adminItems.filter((item) => role && item.roles.includes(role));
  const showAdminSection = filteredAdminItems.length > 0;
  let widthClass = "w-[260px]";
  let CollapseIcon = ChevronLeft;

  if (collapsed) {
    widthClass = "w-[70px]";
    CollapseIcon = ChevronRight;
  }

  return (
    <aside
      className={cn(
        "flex h-full flex-col border-r bg-card transition-all duration-300",
        widthClass
      )}
    >
      <div className="flex h-16 items-center justify-between border-b px-4">
        {!collapsed && (
          <div className="flex flex-col">
            <span className="text-lg font-bold tracking-tight text-foreground">SupportHub</span>
            <span className="text-xs text-muted-foreground">Agentic Router</span>
          </div>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggle}
          className={cn("h-8 w-8", collapsed && "mx-auto")}
        >
          <CollapseIcon className="h-4 w-4" />
        </Button>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3 scrollbar-thin">
        {filteredNavItems.map((item) => (
          <NavItemLink key={item.to} item={item} collapsed={collapsed} />
        ))}

        {showAdminSection && (
          <>
            <Separator className="my-3" />
            {!collapsed && (
              <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Administration
              </div>
            )}
            {filteredAdminItems.map((item) => (
              <NavItemLink key={item.to} item={item} collapsed={collapsed} />
            ))}
          </>
        )}
      </nav>
    </aside>
  );
}
