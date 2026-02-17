import { useLoaderData, useRevalidator } from "react-router-dom";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import type { AdminUsersLoaderData } from "@/router";
import { getQueueMemberships } from "@/app/admin";
import { getTicketMetadata } from "@/app/tickets";
import type { LookupOption, QueueMembershipInfo } from "@/lib/api";
import { getErrorMessage } from "@/lib/api-error";
import { useCreateMembershipMutation, useCreateUserMutation, useDeleteMembershipMutation } from "@/lib/hooks";
import {
  AdminUsersScreen,
  type StaffUserForm,
  type MembershipForm,
  type MembershipFilter,
} from "@/widgets/admin-users/admin-users-screen";

const DEFAULT_STAFF_FORM: StaffUserForm = {
  username: "",
  email: "",
  fullName: "",
  role: "AGENT",
  password: "",
};

const DEFAULT_MEMBERSHIP_FORM: MembershipForm = {
  userId: "",
  queue: "GENERAL_Q",
};

const ALL_QUEUE_FILTER = "ALL";

export default function AdminUsersPage() {
  const data = useLoaderData<AdminUsersLoaderData>();
  const revalidator = useRevalidator();

  const [memberships, setMemberships] = useState<QueueMembershipInfo[]>([]);
  const [queueOptions, setQueueOptions] = useState<LookupOption[]>([]);
  const [membershipFilter, setMembershipFilter] = useState<MembershipFilter>({
    userQuery: "",
    queue: ALL_QUEUE_FILTER,
  });
  const [form, setForm] = useState<StaffUserForm>(DEFAULT_STAFF_FORM);
  const [membershipForm, setMembershipForm] = useState<MembershipForm>(DEFAULT_MEMBERSHIP_FORM);
  const [membershipsLoading, setMembershipsLoading] = useState(false);
  const [membershipLoadingError, setMembershipLoadingError] = useState<string | null>(null);

  const users = useMemo(
    () => [...(data ?? [])].sort((left, right) => left.username.localeCompare(right.username)),
    [data]
  );

  const staffUsers = useMemo(
    () => users.filter((user) => user.role !== "CUSTOMER"),
    [users]
  );

  const membershipAssignableUsers = useMemo(
    () => staffUsers.filter((user) => user.role === "AGENT" && user.active),
    [staffUsers]
  );

  const membershipAlreadyExists = useMemo(() => {
    const targetUserId = Number(membershipForm.userId);
    if (!targetUserId || !membershipForm.queue) {
      return false;
    }

    return memberships.some(
      (membership) => membership.userId === targetUserId && membership.queue === membershipForm.queue
    );
  }, [membershipForm.queue, membershipForm.userId, memberships]);

  const filteredMemberships = useMemo(() => {
    const normalizedQuery = membershipFilter.userQuery.trim().toLowerCase();

    return memberships.filter((membership) => {
      if (membershipFilter.queue !== ALL_QUEUE_FILTER && membership.queue !== membershipFilter.queue) {
        return false;
      }

      if (!normalizedQuery) {
        return true;
      }

      return membership.username.toLowerCase().includes(normalizedQuery);
    });
  }, [membershipFilter.queue, membershipFilter.userQuery, memberships]);

  const createUserMutation = useCreateUserMutation();
  const createMembershipMutation = useCreateMembershipMutation();
  const deleteMembershipMutation = useDeleteMembershipMutation();

  const loadMemberships = useCallback(async () => {
    setMembershipsLoading(true);
    setMembershipLoadingError(null);

    try {
      const membershipsData = await getQueueMemberships();
      const sortedMemberships = [...membershipsData].sort((left, right) => {
        const usernameCompare = left.username.localeCompare(right.username);
        if (usernameCompare !== 0) {
          return usernameCompare;
        }
        return left.queue.localeCompare(right.queue);
      });
      setMemberships(sortedMemberships);
    } catch (error) {
      setMembershipLoadingError(getErrorMessage(error));
    } finally {
      setMembershipsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadMemberships();
  }, [loadMemberships]);

  useEffect(() => {
    const loadQueueOptions = async () => {
      try {
        const metadata = await getTicketMetadata();
        setQueueOptions(metadata.queues ?? []);
      } catch {
        setQueueOptions([]);
      }
    };

    void loadQueueOptions();
  }, []);

  const handleCreateUser = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.username || !form.email || !form.fullName || !form.password) {
      return;
    }

    await createUserMutation.mutateAsync(form);
    setForm(DEFAULT_STAFF_FORM);
    void loadMemberships();
    await revalidator.revalidate();
  };

  const handleCreateMembership = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!membershipForm.userId || membershipAlreadyExists) {
      return;
    }

    await createMembershipMutation.mutateAsync({
      userId: Number(membershipForm.userId),
      queue: membershipForm.queue,
    });
    setMembershipForm(DEFAULT_MEMBERSHIP_FORM);
    void loadMemberships();
  };

  const handleDeleteMembership = async (membershipId: number) => {
    await deleteMembershipMutation.mutateAsync(membershipId);
    void loadMemberships();
  };

  const handleStaffFieldChange = <K extends keyof StaffUserForm>(field: K, value: StaffUserForm[K]) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleMembershipFieldChange = <K extends keyof MembershipForm>(field: K, value: MembershipForm[K]) => {
    setMembershipForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleMembershipFilterChange = <K extends keyof MembershipFilter>(field: K, value: MembershipFilter[K]) => {
    setMembershipFilter((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <AdminUsersScreen
      users={users}
      membershipAssignableUsers={membershipAssignableUsers}
      memberships={memberships}
      filteredMemberships={filteredMemberships}
      queueOptions={queueOptions}
      allQueueFilter={ALL_QUEUE_FILTER}
      form={form}
      membershipForm={membershipForm}
      membershipFilter={membershipFilter}
      membershipsLoading={membershipsLoading}
      membershipLoadingError={membershipLoadingError}
      membershipAlreadyExists={membershipAlreadyExists}
      isCreateUserPending={createUserMutation.isPending}
      isCreateMembershipPending={createMembershipMutation.isPending}
      isDeleteMembershipPending={deleteMembershipMutation.isPending}
      onCreateUser={handleCreateUser}
      onCreateMembership={handleCreateMembership}
      onDeleteMembership={handleDeleteMembership}
      onStaffFieldChange={handleStaffFieldChange}
      onMembershipFieldChange={handleMembershipFieldChange}
      onMembershipFilterChange={handleMembershipFilterChange}
    />
  );
}
