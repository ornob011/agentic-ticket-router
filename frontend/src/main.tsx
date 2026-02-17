import React from "react";
import ReactDOM from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { TooltipProvider } from "@/components/ui/tooltip";
import { queryClient } from "@/lib/queryClient";
import { router } from "@/router";
import { Toaster } from "sonner";
import "./globals.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <RouterProvider router={router} />
        <Toaster
          richColors
          position="top-right"
          closeButton
          toastOptions={{
            classNames: {
              closeButton:
                "!bg-white/80 !border !border-gray-300 !rounded-full !w-5 !h-5 !opacity-100 !right-[-6px] !top-[-6px] !shadow-sm hover:!bg-white hover:!border-gray-400 !transition-colors",
            },
          }}
        />
      </TooltipProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
