import { Outlet } from "react-router-dom";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { getMe } from "@/app/auth";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { loadUserSettings, saveUserSettings } from "@/lib/user-settings";

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(() => loadUserSettings().defaultSidebarCollapsed);
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });
  const handleToggleSidebar = () => {
    const nextValue = !collapsed;
    setCollapsed(nextValue);

    const currentSettings = loadUserSettings();
    saveUserSettings({
      ...currentSettings,
      defaultSidebarCollapsed: nextValue,
    });
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
