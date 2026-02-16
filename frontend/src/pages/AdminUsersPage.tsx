import { useLoaderData, useRevalidator } from "react-router-dom";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import type { AdminUsersLoaderData } from "@/router";
import { api, type LookupOption, type QueueMembershipInfo, type UserRole } from "@/lib/api";
import { getErrorMessage } from "@/lib/api-error";
import { formatLabel } from "@/lib/utils";
import { getRoleBadgeVariant } from "@/lib/role-policy";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Users, Check, X } from "lucide-react";
import { toast } from "sonner";

const DEFAULT_STAFF_FORM = {
  username: "",
  email: "",
  fullName: "",
  role: "AGENT" as UserRole,
  password: "",
};

const DEFAULT_MEMBERSHIP_FORM = {
  userId: "",
  queue: "GENERAL_Q",
};

export default function AdminUsersPage() {
  const data = useLoaderData<AdminUsersLoaderData>();
  const revalidator = useRevalidator();
  const [submitting, setSubmitting] = useState(false);
  const [membershipSubmitting, setMembershipSubmitting] = useState(false);
  const [deletingMembershipId, setDeletingMembershipId] = useState<number | null>(null);
  const [membershipsLoading, setMembershipsLoading] = useState(false);
  const [memberships, setMemberships] = useState<QueueMembershipInfo[]>([]);
  const [queueOptions, setQueueOptions] = useState<LookupOption[]>([]);
  const [membershipFilter, setMembershipFilter] = useState({
    userQuery: "",
    queue: "ALL" as "ALL" | string,
  });
  const [form, setForm] = useState(DEFAULT_STAFF_FORM);
  const [membershipForm, setMembershipForm] = useState(DEFAULT_MEMBERSHIP_FORM);
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
      (membership) =>
        membership.userId === targetUserId &&
        membership.queue === membershipForm.queue
    );
  }, [membershipForm.queue, membershipForm.userId, memberships]);
  const filteredMemberships = useMemo(() => {
    const normalizedQuery = membershipFilter.userQuery.trim().toLowerCase();

    return memberships.filter((membership) => {
      if (membershipFilter.queue !== "ALL") {
        if (membership.queue !== membershipFilter.queue) {
          return false;
        }
      }

      if (!normalizedQuery) {
        return true;
      }

      return membership.username.toLowerCase().includes(normalizedQuery);
    });
  }, [membershipFilter.queue, membershipFilter.userQuery, memberships]);

  const loadMemberships = useCallback(async () => {
    setMembershipsLoading(true);
    try {
      const response = await api.get<QueueMembershipInfo[]>("/admin/queue-memberships");
      const sortedMemberships = [...response.data].sort((left, right) => {
        const usernameCompare = left.username.localeCompare(right.username);
        if (usernameCompare !== 0) {
          return usernameCompare;
        }
        return left.queue.localeCompare(right.queue);
      });
      setMemberships(sortedMemberships);
    } catch (error) {
      toast.error(getErrorMessage(error));
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
        const response = await api.get<{ queues: LookupOption[] }>("/tickets/meta");
        setQueueOptions(response.data.queues ?? []);
      } catch (error) {
        toast.error(getErrorMessage(error));
      }
    };

    void loadQueueOptions();
  }, []);

  const onCreateUser = async (event: FormEvent) => {
    event.preventDefault();
    if (!form.username || !form.email || !form.fullName || !form.password) {
      toast.error("All fields are required");
      return;
    }

    setSubmitting(true);
    try {
      await api.post("/admin/users", form);
      setForm(DEFAULT_STAFF_FORM);
      revalidator.revalidate();
      await loadMemberships();
      toast.success("Staff user created");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const onCreateMembership = async (event: FormEvent) => {
    event.preventDefault();
    if (!membershipForm.userId) {
      toast.error("Select a user");
      return;
    }
    if (membershipAlreadyExists) {
      toast.error("Membership already exists");
      return;
    }

    setMembershipSubmitting(true);
    try {
      await api.post("/admin/queue-memberships", {
        userId: Number(membershipForm.userId),
        queue: membershipForm.queue,
      });
      setMembershipForm(DEFAULT_MEMBERSHIP_FORM);
      await loadMemberships();
      toast.success("Queue membership added");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setMembershipSubmitting(false);
    }
  };

  const onDeleteMembership = async (membershipId: number) => {
    setDeletingMembershipId(membershipId);
    try {
      await api.delete(`/admin/queue-memberships/${membershipId}`);
      await loadMemberships();
      toast.success("Queue membership removed");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setDeletingMembershipId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Users</h1>
        <p className="text-muted-foreground">Manage staff and customer accounts</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Create Staff User</CardTitle>
          <CardDescription>Create AGENT, SUPERVISOR, or ADMIN accounts</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onCreateUser} className="grid gap-3 md:grid-cols-2">
            <Input
              placeholder="Username"
              value={form.username}
              onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
            />
            <Input
              placeholder="Email"
              type="email"
              value={form.email}
              onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            />
            <Input
              placeholder="Full name"
              value={form.fullName}
              onChange={(event) => setForm((prev) => ({ ...prev, fullName: event.target.value }))}
            />
            <Input
              placeholder="Temporary password"
              type="password"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            />
            <Select
              value={form.role}
              onValueChange={(value: UserRole) => setForm((prev) => ({ ...prev, role: value }))}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select role" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="AGENT">Agent</SelectItem>
                <SelectItem value="SUPERVISOR">Supervisor</SelectItem>
                <SelectItem value="ADMIN">Admin</SelectItem>
              </SelectContent>
            </Select>
            <div className="flex items-end">
              <Button type="submit" disabled={submitting}>
                {submitting ? "Creating..." : "Create User"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Queue Memberships</CardTitle>
          <CardDescription>Define which queues each staff user can work on</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={onCreateMembership} className="grid gap-3 md:grid-cols-3">
            <Select
              value={membershipForm.userId}
              onValueChange={(value) => setMembershipForm((prev) => ({ ...prev, userId: value }))}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select agent" />
              </SelectTrigger>
              <SelectContent>
                {membershipAssignableUsers.map((staffUser) => (
                  <SelectItem key={staffUser.id} value={String(staffUser.id)}>
                    {staffUser.fullName || staffUser.username}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={membershipForm.queue}
              onValueChange={(value) => {
                setMembershipForm((prev) => ({ ...prev, queue: value }));
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select queue" />
              </SelectTrigger>
              <SelectContent>
                {queueOptions.map((queueOption) => (
                  <SelectItem key={queueOption.code} value={queueOption.code}>
                    {queueOption.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <div className="flex items-end">
              <Button
                type="submit"
                disabled={
                  membershipSubmitting || membershipAlreadyExists || membershipAssignableUsers.length === 0
                }
              >
                {membershipSubmitting ? "Adding..." : "Add Membership"}
              </Button>
            </div>
          </form>
          {membershipAssignableUsers.length === 0 && (
            <p className="text-sm text-amber-700">No active agents available for queue membership assignment.</p>
          )}
          {membershipAlreadyExists && (
            <p className="text-sm text-amber-700">
              Selected user already has membership for this queue.
            </p>
          )}

          <div className="grid gap-3 md:grid-cols-2">
            <Input
              placeholder="Filter by username"
              value={membershipFilter.userQuery}
              onChange={(event) => setMembershipFilter((prev) => ({ ...prev, userQuery: event.target.value }))}
            />
            <Select
              value={membershipFilter.queue}
              onValueChange={(value) => {
                setMembershipFilter((prev) => ({ ...prev, queue: value as "ALL" | string }));
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Filter by queue" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">ALL</SelectItem>
                {queueOptions.map((queueOption) => (
                  <SelectItem key={queueOption.code} value={queueOption.code}>
                    {queueOption.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead>Queue</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredMemberships.map((membership) => (
                <TableRow key={membership.id}>
                  <TableCell>{membership.username}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{membership.queueLabel}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      type="button"
                      size="sm"
                      variant="destructive"
                      disabled={deletingMembershipId === membership.id}
                      onClick={() => void onDeleteMembership(membership.id)}
                    >
                      {deletingMembershipId === membership.id ? "Removing..." : "Remove"}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {!membershipsLoading && filteredMemberships.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground">
                    No memberships match the current filters.
                  </TableCell>
                </TableRow>
              )}
              {membershipsLoading && (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground">
                    Loading queue memberships...
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            All Users
          </CardTitle>
          <CardDescription>System users and their access levels</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {users.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User</TableHead>
                  <TableHead className="hidden md:table-cell">Email</TableHead>
                  <TableHead className="hidden lg:table-cell">Role</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => {
                  const displayName = user.fullName || user.username;
                  const roleLabel = user.roleLabel || formatLabel(user.role);
                  const showUsername = Boolean(user.fullName);
                  let activityVariant: "success" | "secondary" = "secondary";
                  let activityLabel = "Disabled";
                  let ActivityIcon = X;

                  if (user.active) {
                    activityVariant = "success";
                    activityLabel = "Active";
                    ActivityIcon = Check;
                  }

                  return (
                    <TableRow key={user.id}>
                      <TableCell>
                        <div>
                          <p className="font-medium">{displayName}</p>
                          {showUsername && (
                            <p className="text-sm text-muted-foreground">{user.username}</p>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="hidden md:table-cell text-muted-foreground">
                        {user.email}
                      </TableCell>
                      <TableCell className="hidden lg:table-cell">
                        <Badge variant={getRoleBadgeVariant(user.role)}>
                          {roleLabel}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={activityVariant}>
                          <ActivityIcon className="mr-1 h-3 w-3" />
                          {activityLabel}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          ) : (
            <div className="p-6 text-sm text-muted-foreground">No users found.</div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
