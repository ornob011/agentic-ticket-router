import type { FormEvent } from "react";
import type { LookupOption, QueueMembershipInfo, UserInfo, UserRole } from "@/lib/api";
import { formatLabel } from "@/lib/utils";
import { getRoleBadgeVariant } from "@/lib/role-policy";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Users, Check, X } from "lucide-react";

export type StaffUserForm = {
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
  password: string;
};

export type MembershipForm = {
  userId: string;
  queue: string;
};

export type MembershipFilter = {
  userQuery: string;
  queue: string;
};

export type AdminUsersScreenProps = Readonly<{
  users: UserInfo[];
  membershipAssignableUsers: UserInfo[];
  memberships: QueueMembershipInfo[];
  filteredMemberships: QueueMembershipInfo[];
  queueOptions: LookupOption[];
  allQueueFilter: string;
  form: StaffUserForm;
  membershipForm: MembershipForm;
  membershipFilter: MembershipFilter;
  membershipsLoading: boolean;
  membershipLoadingError: string | null;
  membershipAlreadyExists: boolean;
  createUserPending: boolean;
  createMembershipPending: boolean;
  deleteMembershipPending: boolean;
  onCreateUser: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onCreateMembership: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onDeleteMembership: (membershipId: number) => Promise<void>;
  onStaffFieldChange: <K extends keyof StaffUserForm>(field: K, value: StaffUserForm[K]) => void;
  onMembershipFieldChange: <K extends keyof MembershipForm>(field: K, value: MembershipForm[K]) => void;
  onMembershipFilterChange: <K extends keyof MembershipFilter>(field: K, value: MembershipFilter[K]) => void;
}>;

export function AdminUsersScreen(props: AdminUsersScreenProps) {
  const {
    users,
    membershipAssignableUsers,
    memberships,
    filteredMemberships,
    queueOptions,
    allQueueFilter,
    form,
    membershipForm,
    membershipFilter,
    membershipsLoading,
    membershipLoadingError,
    membershipAlreadyExists,
    createUserPending,
    createMembershipPending,
    deleteMembershipPending,
    onCreateUser,
    onCreateMembership,
    onDeleteMembership,
    onStaffFieldChange,
    onMembershipFieldChange,
    onMembershipFilterChange,
  } = props;

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
          <form onSubmit={(event) => void onCreateUser(event)} className="grid gap-3 md:grid-cols-2">
            <Input
              placeholder="Username"
              value={form.username}
              onChange={(event) => onStaffFieldChange("username", event.target.value)}
            />
            <Input
              placeholder="Email"
              type="email"
              value={form.email}
              onChange={(event) => onStaffFieldChange("email", event.target.value)}
            />
            <Input
              placeholder="Full name"
              value={form.fullName}
              onChange={(event) => onStaffFieldChange("fullName", event.target.value)}
            />
            <Input
              placeholder="Temporary password"
              type="password"
              value={form.password}
              onChange={(event) => onStaffFieldChange("password", event.target.value)}
            />
            <Select value={form.role} onValueChange={(value: UserRole) => onStaffFieldChange("role", value)}>
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
              <Button type="submit" disabled={createUserPending}>
                {createUserPending ? "Creating..." : "Create User"}
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
          <form onSubmit={(event) => void onCreateMembership(event)} className="grid gap-3 md:grid-cols-3">
            <Select value={membershipForm.userId} onValueChange={(value) => onMembershipFieldChange("userId", value)}>
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
            <Select value={membershipForm.queue} onValueChange={(value) => onMembershipFieldChange("queue", value)}>
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
                  createMembershipPending || membershipAlreadyExists || membershipAssignableUsers.length === 0
                }
              >
                {createMembershipPending ? "Adding..." : "Add Membership"}
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
              onChange={(event) => onMembershipFilterChange("userQuery", event.target.value)}
            />
            <Select value={membershipFilter.queue} onValueChange={(value) => onMembershipFilterChange("queue", value)}>
              <SelectTrigger>
                <SelectValue placeholder="Filter by queue" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={allQueueFilter}>{allQueueFilter}</SelectItem>
                {queueOptions.map((queueOption) => (
                  <SelectItem key={queueOption.code} value={queueOption.code}>
                    {queueOption.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {membershipLoadingError && (
            <p className="text-sm text-destructive">{membershipLoadingError}</p>
          )}

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
                      disabled={deleteMembershipPending}
                      onClick={() => void onDeleteMembership(membership.id)}
                    >
                      {deleteMembershipPending ? "Removing..." : "Remove"}
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

          {memberships.length === 0 && !membershipsLoading && !membershipLoadingError && (
            <p className="text-sm text-muted-foreground">No queue memberships configured yet.</p>
          )}
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
