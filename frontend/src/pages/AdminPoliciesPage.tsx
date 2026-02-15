import { useLoaderData, useRevalidator } from "react-router-dom";
import { useState } from "react";
import type { AdminPoliciesLoaderData } from "@/router";
import { api } from "@/lib/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Settings, Check, X } from "lucide-react";
import { toast } from "sonner";

export default function AdminPoliciesPage() {
  const data = useLoaderData<AdminPoliciesLoaderData>();
  const revalidator = useRevalidator();
  const [values, setValues] = useState<Record<string, string>>({});
  const [savingKey, setSavingKey] = useState<string | null>(null);

  const policies = data ?? [];

  const onUpdatePolicy = async (configKey: string, value: string) => {
    const configValue = Number(value);
    if (Number.isNaN(configValue)) {
      toast.error("Config value must be a number");
      return;
    }

    setSavingKey(configKey);
    try {
      await api.patch("/admin/policy-config", {
        configKey,
        configValue,
      });
      await revalidator.revalidate();
      toast.success(`${configKey} updated`);
    } catch (error) {
      toast.error("Failed to update policy");
      console.error("Failed to update policy:", error);
    } finally {
      setSavingKey(null);
    }
  };

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
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            value={values[policy.configKey] ?? String(policy.configValue)}
                            onChange={(event) => {
                              setValues((prev) => ({
                                ...prev,
                                [policy.configKey]: event.target.value,
                              }));
                            }}
                            className="w-36"
                          />
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={savingKey === policy.configKey}
                            onClick={() => void onUpdatePolicy(
                              policy.configKey,
                              values[policy.configKey] ?? String(policy.configValue)
                            )}
                          >
                            {savingKey === policy.configKey ? "Saving..." : "Save"}
                          </Button>
                        </div>
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
