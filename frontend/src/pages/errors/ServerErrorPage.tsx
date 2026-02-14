import { ServerCrash } from "lucide-react";
import { ErrorPageLayout } from "./ErrorPageLayout";

export default function ServerErrorPage() {
  return (
    <ErrorPageLayout
      icon={<ServerCrash className="h-8 w-8 text-destructive" />}
      title="Server Error"
      description="An unexpected error occurred"
      code="500"
    />
  );
}
