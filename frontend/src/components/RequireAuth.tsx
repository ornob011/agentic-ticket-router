import { useQuery } from "@tanstack/react-query";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isAxiosError } from "axios";
import { getMe } from "@/app/auth";

export default function RequireAuth() {
  const location = useLocation();
  const { isLoading, error, isError } = useQuery({ 
    queryKey: ["me"], 
    queryFn: getMe,
    retry: false,
    refetchOnWindowFocus: false,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-4">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
          <p className="text-sm text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  if (isError) {
    const isUnauthorized = isAxiosError(error) && error.response?.status === 401;
    if (isUnauthorized) {
      return <Navigate to="/app/login" state={{ from: location }} replace />;
    }
  }

  return <Outlet />;
}
