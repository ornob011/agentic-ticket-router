import { useLoaderData, useRevalidator } from "react-router-dom";
import { useEffect, useState } from "react";
import { AxiosError } from "axios";
import type { AdminPoliciesLoaderData } from "@/router";
import { api } from "@/lib/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Settings, Check, X, RotateCcw } from "lucide-react";
import { toast } from "sonner";

type PolicyActionType = "save" | "toggle" | "reset";

type ProblemDetailError = {
  detail?: string;
  fieldErrors?: Record<string, string>;
  globalErrors?: string[];
};

export default function AdminPoliciesPage() {
  const data = useLoaderData<AdminPoliciesLoaderData>();
  const revalidator = useRevalidator();
  const policies = data ?? [];
  const [values, setValues] = useState<Record<string, string>>({});
  const [actionState, setActionState] = useState<{
    key: string;
    action: PolicyActionType;
  } | null>(null);

  useEffect(() => {
    setValues(
      Object.fromEntries(
        policies.map((policy) => [policy.configKey, String(policy.configValue)])
      )
    );
  }, [policies]);

  const errorMessage = (rawError: unknown, fallbackMessage: string) => {
    if (!(rawError instanceof AxiosError)) {
      return fallbackMessage;
    }

    const payload = rawError.response?.data as ProblemDetailError | undefined;
    if (!payload) {
      return fallbackMessage;
    }

    const firstFieldError = payload.fieldErrors ? Object.values(payload.fieldErrors)[0] : null;
    const firstGlobalError = payload.globalErrors?.[0];
    return firstFieldError || firstGlobalError || payload.detail || fallbackMessage;
  };

  const onUpdatePolicy = async (configKey: string, value: string) => {
    const configValue = Number(value);
    if (Number.isNaN(configValue)) {
      toast.error("Config value must be a number");
      return;
    }

    setActionState({
      key: configKey,
      action: "save",
    });
    try {
      await api.patch("/admin/policy-config", {
        configKey,
        configValue,
      });
      await revalidator.revalidate();
      toast.success(`${configKey} updated`);
    } catch (rawError) {
      toast.error(errorMessage(rawError, "Failed to update policy"));
      console.error("Failed to update policy:", rawError);
    } finally {
      setActionState(null);
    }
  };

  const onUpdatePolicyStatus = async (
    configKey: string,
    active: boolean
  ) => {
    setActionState({
      key: configKey,
      action: "toggle",
    });
    try {
      await api.patch("/admin/policy-config/status", {
        configKey,
        active,
      });
      await revalidator.revalidate();
      toast.success(`${configKey} ${active ? "activated" : "deactivated"}`);
    } catch (rawError) {
      toast.error(errorMessage(rawError, "Failed to update policy status"));
      console.error("Failed to update policy status:", rawError);
    } finally {
      setActionState(null);
    }
  };

  const onResetPolicy = async (configKey: string) => {
    setActionState({
      key: configKey,
      action: "reset",
    });

    try {
      await api.post(`/admin/policy-config/${configKey}/reset`);
      await revalidator.revalidate();
      toast.success(`${configKey} reset to default`);
    } catch (rawError) {
      toast.error(errorMessage(rawError, "Failed to reset policy"));
      console.error("Failed to reset policy:", rawError);
    } finally {
      setActionState(null);
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
                  <TableHead>Policy Config Key</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Value</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
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
                        <div className="space-y-1">
                          <span className="font-mono text-sm">{policy.configKey}</span>
                          <div className="text-xs text-muted-foreground">{policy.configKeyLabel}</div>
                        </div>
                      </TableCell>
                      <TableCell className="max-w-xl">
                        <span className="text-sm text-muted-foreground">
                          {policy.description || policy.configKeyLabel}
                        </span>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-2">
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
                              className="w-40"
                            />
                          </div>
                          <div className="text-xs text-muted-foreground">
                            Min: {policy.minValue ?? "-"} | Max: {policy.maxValue ?? "-"} | Default: {policy.defaultValue ?? "-"}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={activityVariant}>
                          <ActivityIcon className="mr-1 h-3 w-3" />
                          {activityLabel}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={actionState?.key === policy.configKey}
                            onClick={() =>
                              void onUpdatePolicy(
                                policy.configKey,
                                values[policy.configKey] ?? String(policy.configValue)
                              )
                            }
                          >
                            {actionState?.key === policy.configKey && actionState.action === "save"
                              ? "Saving..."
                              : "Save"}
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="secondary"
                            disabled={
                              actionState?.key === policy.configKey ||
                              policy.defaultValue === null
                            }
                            onClick={() => void onResetPolicy(policy.configKey)}
                          >
                            {actionState?.key === policy.configKey && actionState.action === "reset" ? (
                              "Resetting..."
                            ) : (
                              <>
                                <RotateCcw className="mr-1 h-3.5 w-3.5" />
                                Reset
                              </>
                            )}
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant={policy.active ? "destructive" : "default"}
                            disabled={actionState?.key === policy.configKey}
                            onClick={() => void onUpdatePolicyStatus(policy.configKey, !policy.active)}
                          >
                            {actionState?.key === policy.configKey && actionState.action === "toggle"
                              ? "Updating..."
                              : policy.active
                                ? "Deactivate"
                                : "Activate"}
                          </Button>
                        </div>
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
