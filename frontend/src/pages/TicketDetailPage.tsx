import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useLoaderData, useNavigate, useRevalidator } from "react-router-dom";
import type { TicketDetailLoaderData } from "@/router";
import {
  useAddReplyMutation,
  useAssignSelfMutation,
  usePeriodicRevalidation,
  useUpdateTicketStatusMutation,
} from "@/lib/hooks";
import { TicketDetailScreen } from "@/widgets/ticket-detail/ticket-detail-screen";

export default function TicketDetailPage() {
  const data = useLoaderData<TicketDetailLoaderData>();
  const navigate = useNavigate();
  const revalidator = useRevalidator();

  const [reply, setReply] = useState("");
  const [newStatus, setNewStatus] = useState<string>(data.status);
  const [statusReason, setStatusReason] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  usePeriodicRevalidation(revalidator);

  const replyMutation = useAddReplyMutation();
  const statusMutation = useUpdateTicketStatusMutation();
  const assignSelfMutation = useAssignSelfMutation();

  useEffect(() => {
    setNewStatus(data.status);
    setStatusReason("");
    setValidationError(null);
  }, [data.status]);

  const handleReplySubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!reply.trim() || replyMutation.isPending || !data.permissions.canReply) {
      return;
    }

    await replyMutation.mutateAsync({
      ticketId: data.id,
      content: reply,
    });
    setReply("");
  };

  const handleUpdateStatus = async () => {
    setValidationError(null);

    if (statusMutation.isPending || newStatus === data.status) {
      return;
    }

    if (newStatus === "ESCALATED" && !statusReason.trim()) {
      setValidationError("Escalation reason is required.");
      return;
    }

    await statusMutation.mutateAsync({
      ticketId: data.id,
      newStatus,
      reason: statusReason.trim() || undefined,
    });
    setStatusReason("");
  };

  const handleStatusReasonChange = (value: string) => {
    setStatusReason(value);
    setValidationError(null);
  };

  const handleAssignSelf = async () => {
    await assignSelfMutation.mutateAsync({ ticketId: data.id });
  };

  return (
    <TicketDetailScreen
      data={data}
      reply={reply}
      onReplyChange={setReply}
      onReplySubmit={handleReplySubmit}
      isReplyPending={replyMutation.isPending}
      newStatus={newStatus}
      onStatusChange={setNewStatus}
      statusReason={statusReason}
      onStatusReasonChange={handleStatusReasonChange}
      onUpdateStatus={handleUpdateStatus}
      isStatusPending={statusMutation.isPending}
      validationError={validationError}
      onAssignSelf={handleAssignSelf}
      isAssignPending={assignSelfMutation.isPending}
      onBack={() => navigate(-1)}
    />
  );
}
