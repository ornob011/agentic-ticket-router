import { Outlet } from "react-router-dom";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { getMe, getSettings, updateSettings } from "@/app/auth";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useEffect } from "react";

export function AppLayout() {
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });
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
        <Sidebar role={me?.role} collapsed={collapsed} onToggle={handleToggleSidebar} />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
