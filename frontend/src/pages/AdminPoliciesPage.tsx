import { useQuery } from "@tanstack/react-query";
import { api, type PolicyInfo } from "@/lib/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Settings, Check, X } from "lucide-react";

function PoliciesSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-48" />
        </CardContent>
      </Card>
    </div>
  );
}

export default function AdminPoliciesPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin-policies"],
    queryFn: async () => (await api.get<PolicyInfo[]>("/admin/policy-config")).data,
  });

  if (isLoading) {
    return <PoliciesSkeleton />;
  }

  const policies = data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Policy Configuration</h1>
        <p className="text-muted-foreground">Runtime thresholds and gates for the routing engine</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Settings className="h-5 w-5" />
            Policy Settings
          </CardTitle>
          <CardDescription>Configuration values that control routing behavior</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {policies.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Configuration Key</TableHead>
                  <TableHead>Value</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {policies.map((policy) => {
                  let activityVariant: "success" | "secondary" = "secondary";
                  let activityLabel = "Inactive";
                  let ActivityIcon = X;

                  if (policy.active) {
                    activityVariant = "success";
                    activityLabel = "Active";
                    ActivityIcon = Check;
                  }

                  return (
                    <TableRow key={policy.id}>
                      <TableCell>
                        <span className="font-mono text-sm">{policy.configKey}</span>
                      </TableCell>
                      <TableCell>
                        <span className="font-medium">{policy.configValue}</span>
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
