import { useRouteError, isRouteErrorResponse } from "react-router-dom";
import { parseApiError } from "@/lib/api-error";
import { FileQuestion, ShieldX, ServerCrash, AlertTriangle } from "lucide-react";
import { AppErrorContent } from "@/components/errors/AppErrorContent";

type ErrorMeta = {
  icon: typeof FileQuestion;
  iconClass: string;
  title: string;
  description: string;
  code: string;
  detail: string;
};

function getErrorMeta(error: unknown): ErrorMeta {
  if (isRouteErrorResponse(error)) {
    switch (error.status) {
      case 403:
        return {
          icon: ShieldX,
          iconClass: "text-destructive",
          title: "Access Denied",
          description: "You don't have permission to access this page",
          code: "403",
          detail: "You don't have the necessary permissions to access this resource.",
        };
      case 404:
        return {
          icon: FileQuestion,
          iconClass: "text-muted-foreground",
          title: "Page Not Found",
          description: "The requested page could not be found",
          code: "404",
          detail: "The page you're looking for doesn't exist or may have moved.",
        };
      case 500:
        return {
          icon: ServerCrash,
          iconClass: "text-destructive",
          title: "Server Error",
          description: "An unexpected error occurred",
          code: "500",
          detail: "Something went wrong on our end. Please try again in a moment.",
        };
      default:
        return {
          icon: AlertTriangle,
          iconClass: "text-warning",
          title: `Error ${error.status}`,
          description: error.statusText || "An error occurred",
          code: String(error.status),
          detail: error.statusText || "An unexpected error occurred while loading this page.",
        };
    }
  }

  const apiError = parseApiError(error);
  if (apiError.status > 0) {
    switch (apiError.status) {
      case 403:
        return {
          icon: ShieldX,
          iconClass: "text-destructive",
          title: "Access Denied",
          description: apiError.detail || "You don't have permission",
          code: "403",
          detail: apiError.detail || "You don't have the necessary permissions to access this resource.",
        };
      case 404:
        return {
          icon: FileQuestion,
          iconClass: "text-muted-foreground",
          title: "Not Found",
          description: apiError.detail || "Resource not found",
          code: "404",
          detail: apiError.detail || "The requested resource could not be found.",
        };
      case 500:
      default:
        return {
          icon: ServerCrash,
          iconClass: "text-destructive",
          title: "Server Error",
          description: apiError.detail || "An unexpected error occurred",
          code: "500",
          detail: apiError.detail || "Something went wrong on our end. Please try again in a moment.",
        };
    }
  }

  return {
    icon: AlertTriangle,
    iconClass: "text-warning",
    title: "Unexpected Error",
    description: "Something went wrong",
    code: "ERR",
    detail: "An unexpected error occurred while loading this page.",
  };
}

export default function RouteErrorBoundary() {
  const error = useRouteError();
  const parsedApiError = parseApiError(error);
  const apiErrorCode = parsedApiError.status > 0 ? parsedApiError.code : undefined;
  const meta = getErrorMeta(error);
  const Icon = meta.icon;

  return (
    <AppErrorContent
      icon={<Icon className={`h-8 w-8 ${meta.iconClass}`} />}
      title={meta.title}
      description={meta.description}
      code={meta.code}
      detail={meta.detail}
      apiErrorCode={apiErrorCode}
      showRetry={meta.code === "500"}
    />
  );
}
