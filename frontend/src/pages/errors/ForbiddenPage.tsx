import { ShieldX } from "lucide-react";
import { ErrorPageLayout } from "./ErrorPageLayout";

export default function ForbiddenPage() {
  return (
    <ErrorPageLayout
      icon={<ShieldX className="h-8 w-8 text-destructive" />}
      title="Access Denied"
      description="You don't have permission to access this resource"
      code="403"
    />
  );
}
