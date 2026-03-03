import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useLoaderData, useNavigate } from "react-router-dom";
import type { EscalationDetailLoaderData } from "@/router";
import { useAssignableSupervisors, useAssignEscalationSupervisorMutation, useResolveEscalationMutation } from "@/lib/hooks";
import { appRoutes } from "@/lib/routes";
import { EscalationDetailScreen } from "@/widgets/escalation-detail/escalation-detail-screen";

export default function EscalationDetailPage() {
  const data = useLoaderData<EscalationDetailLoaderData>();
  const navigate = useNavigate();
  const [resolutionNotes, setResolutionNotes] = useState("");
  const [selectedSupervisorId, setSelectedSupervisorId] = useState("");

  const { assignableSupervisors } = useAssignableSupervisors(!data.resolved);
  const assignSupervisorMutation = useAssignEscalationSupervisorMutation();
  const resolveMutation = useResolveEscalationMutation();

  useEffect(() => {
    if (!data.assignedSupervisor) {
      return;
    }

    const matchedSupervisor = assignableSupervisors.find(
      (supervisor) => (supervisor.fullName || supervisor.username) === data.assignedSupervisor
    );

    if (matchedSupervisor) {
      setSelectedSupervisorId(String(matchedSupervisor.id));
    }
  }, [assignableSupervisors, data.assignedSupervisor]);

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

  const onAssignSupervisor = async () => {
    if (!selectedSupervisorId || assignSupervisorMutation.isPending) {
      return;
    }

    const supervisor = assignableSupervisors.find((item) => item.id === Number(selectedSupervisorId));
    await assignSupervisorMutation.mutateAsync({
      escalationId: data.id,
      supervisorId: Number(selectedSupervisorId),
      supervisorName: supervisor?.fullName || supervisor?.username,
    });
  };

  return (
    <EscalationDetailScreen
      escalation={data}
      assignableSupervisors={assignableSupervisors}
      selectedSupervisorId={selectedSupervisorId}
      resolutionNotes={resolutionNotes}
      isAssigningSupervisor={assignSupervisorMutation.isPending}
      isResolving={resolveMutation.isPending}
      onSelectedSupervisorIdChange={setSelectedSupervisorId}
      onAssignSupervisor={onAssignSupervisor}
      onResolutionNotesChange={setResolutionNotes}
      onResolve={onResolve}
      onBack={() => navigate(-1)}
      onViewTicket={() => void navigate(appRoutes.agent.ticketDetail(data.ticketId))}
    />
  );
}
