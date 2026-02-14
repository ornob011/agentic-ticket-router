import { useQuery } from "@tanstack/react-query";
import { api, type UserInfo } from "@/lib/api";
import { formatLabel } from "@/lib/utils";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Users, Check, X } from "lucide-react";

function UsersSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-16" />
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function getRoleBadgeVariant(role: string): "default" | "destructive" | "warning" | "secondary" {
  switch (role) {
    case "ADMIN":
      return "destructive";
    case "SUPERVISOR":
      return "warning";
    case "AGENT":
      return "default";
    default:
      return "secondary";
  }
}

export default function AdminUsersPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin-users"],
    queryFn: async () => (await api.get<UserInfo[]>("/admin/users")).data,
  });

  if (isLoading) {
    return <UsersSkeleton />;
  }

  const users = data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Users</h1>
        <p className="text-muted-foreground">Manage staff and customer accounts</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            All Users
          </CardTitle>
          <CardDescription>System users and their access levels</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {users.length > 0 && (
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
          )}
        </CardContent>
      </Card>
    </div>
  );
}
