import { Outlet } from "react-router-dom";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { getMe } from "@/app/auth";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });

  return (
    <div className="flex h-screen overflow-hidden bg-muted/30">
      <div className="hidden lg:flex">
        <Sidebar role={me?.role} collapsed={collapsed} onToggle={() => setCollapsed((current) => !current)} />
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
