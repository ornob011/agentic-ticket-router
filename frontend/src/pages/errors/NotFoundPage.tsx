import { FileQuestion } from "lucide-react";
import { ErrorPageLayout } from "./ErrorPageLayout";

export default function NotFoundPage() {
  return (
    <ErrorPageLayout
      icon={<FileQuestion className="h-8 w-8 text-muted-foreground" />}
      title="Page Not Found"
      description="The requested page could not be found"
      code="404"
    />
  );
}
