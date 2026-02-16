import type { ReactNode } from "react";
import { Outlet, useRouteLoaderData } from "react-router-dom";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { updateSettings, getSettings } from "@/app/auth";
import { useState, useEffect } from "react";
import type { RootLoaderData } from "@/router";
import { useQueryClient, useQuery } from "@tanstack/react-query";

type AppLayoutProps = {
  children?: ReactNode;
};

export function AppLayout({ children }: Readonly<AppLayoutProps>) {
  const queryClient = useQueryClient();
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const { data: settings } = useQuery({ queryKey: ["settings"], queryFn: getSettings });
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (settings?.sidebarCollapsed !== undefined) {
      setCollapsed(settings.sidebarCollapsed);
    }
  }, [settings?.sidebarCollapsed]);

  const handleToggleSidebar = async () => {
    const nextValue = !collapsed;
    setCollapsed(nextValue);
    try {
      await updateSettings({ sidebarCollapsed: nextValue });
      queryClient.setQueryData(["settings"], (old: unknown) => {
        if (typeof old === "object" && old !== null) {
          return { ...old, sidebarCollapsed: nextValue };
        }
        return old;
      });
    } catch {
      setCollapsed(!nextValue);
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-muted/30">
      <div className="hidden lg:flex">
        <Sidebar role={appData?.user?.role} collapsed={collapsed} onToggle={handleToggleSidebar} />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <div className="mx-auto w-full">
            {children ?? <Outlet/>}
          </div>
        </main>
      </div>
    </div>
  );
}
