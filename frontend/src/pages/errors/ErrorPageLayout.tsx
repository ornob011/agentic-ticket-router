import type { ReactNode } from "react";
import { AppErrorContent } from "@/components/errors/AppErrorContent";

type Props = {
  icon: ReactNode;
  title: string;
  description: string;
  code: string;
};

function resolveErrorDetail(code: string): string {
  switch (code) {
    case "404":
      return "The page you're looking for doesn't exist or has been moved. Please check the URL or navigate back.";
    case "403":
      return "You don't have the necessary permissions to access this resource. Contact your administrator if this seems incorrect.";
    case "500":
      return "Something went wrong on our end. Please retry or return to your dashboard.";
    default:
      return "An unexpected error occurred.";
  }
}

export function ErrorPageLayout({
  icon,
  title,
  description,
  code,
}: Readonly<Props>) {
  const detail = resolveErrorDetail(code);

  return (
    <AppErrorContent
      icon={icon}
      title={title}
      description={description}
      code={code}
      detail={detail}
      showRetry={code === "500"}
    />
  );
}
