import type { FormEvent } from "react";
import { useState } from "react";
import { useLoaderData, useNavigate } from "react-router-dom";
import type { EscalationDetailLoaderData } from "@/router";
import { useResolveEscalationMutation } from "@/lib/hooks";
import { appRoutes } from "@/lib/routes";
import { EscalationDetailScreen } from "@/widgets/escalation-detail/escalation-detail-screen";

export default function EscalationDetailPage() {
  const data = useLoaderData<EscalationDetailLoaderData>();
  const navigate = useNavigate();
  const [resolutionNotes, setResolutionNotes] = useState("");

  const resolveMutation = useResolveEscalationMutation();

  const onResolve = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!resolutionNotes.trim() || resolveMutation.isPending) {
      return;
    }

    await resolveMutation.mutateAsync({
      escalationId: data.id,
      resolutionNotes,
    });
    setResolutionNotes("");
  };

  return (
    <EscalationDetailScreen
      escalation={data}
      resolutionNotes={resolutionNotes}
      isResolving={resolveMutation.isPending}
      onResolutionNotesChange={setResolutionNotes}
      onResolve={onResolve}
      onBack={() => navigate(-1)}
      onViewTicket={() => void navigate(appRoutes.agent.ticketDetail(data.ticketId))}
    />
  );
}
