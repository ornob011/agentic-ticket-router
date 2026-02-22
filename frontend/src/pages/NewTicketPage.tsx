import type { FormEvent } from "react";
import { useState } from "react";
import { Navigate, useNavigate, useRevalidator, useRouteLoaderData } from "react-router-dom";
import { useCreateTicketMutation } from "@/lib/hooks";
import { isCustomerRole } from "@/lib/role-policy";
import { appRoutes } from "@/lib/routes";
import type { RootLoaderData } from "@/router";
import { NewTicketScreen } from "@/widgets/new-ticket/new-ticket-screen";

export default function NewTicketPage() {
  const appData = useRouteLoaderData<RootLoaderData>("app");
  const isCustomer = isCustomerRole(appData?.user?.role);
  const navigate = useNavigate();
  const revalidator = useRevalidator();

  const [subject, setSubject] = useState("");
  const [content, setContent] = useState("");
  const [validationError, setValidationError] = useState("");

  const createTicketMutation = useCreateTicketMutation();

  if (!isCustomer) {
    return <Navigate to={appRoutes.errors.forbidden} replace />;
  }

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setValidationError("");

    if (!subject.trim() || !content.trim()) {
      setValidationError("Please fill in all fields");
      return;
    }

    try {
      const response = await createTicketMutation.mutateAsync({
        subject: subject.trim(),
        content: content.trim(),
      });
      await revalidator.revalidate();
      void navigate(
        appRoutes.tickets.detail(response.id),
        {
          state: {
            activateRoutingPanel: true,
          },
        }
      );
    } catch {
      setValidationError("Unable to submit ticket right now. Please try again.");
    }
  };

  return (
    <NewTicketScreen
      subject={subject}
      content={content}
      validationError={validationError}
      isSubmitting={createTicketMutation.isPending}
      onSubjectChange={setSubject}
      onContentChange={setContent}
      onSubmit={onSubmit}
      onCancel={() => void navigate(appRoutes.tickets.list)}
    />
  );
}
