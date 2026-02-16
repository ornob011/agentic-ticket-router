import { useLoaderData, useRevalidator } from "react-router-dom";
import { useEffect, useState } from "react";
import type { AdminPoliciesLoaderData } from "@/router";
import { api } from "@/lib/api";
import { getErrorMessage } from "@/lib/api-error";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Settings, RotateCcw } from "lucide-react";
import { toast } from "sonner";

type PolicyActionType = "save" | "toggle" | "reset";

function getActionLabel(action: PolicyActionType): string {
  const labels: Record<PolicyActionType, string> = {
    save: "Saving...",
    toggle: "Updating...",
    reset: "Resetting...",
  };
  return labels[action];
}

function getToggleToastMessage(configKey: string, isActive: boolean): string {
  return `${configKey} ${isActive ? "activated" : "deactivated"}`;
}

function getToggleButtonLabel(isActive: boolean): string {
  return isActive ? "Deactivate" : "Activate";
}

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

  const isPolicyLoading = (configKey: string): boolean => {
    return actionState?.key === configKey;
  };

  const getPolicyAction = (configKey: string): PolicyActionType | null => {
    if (actionState?.key === configKey) {
      return actionState.action;
    }
    return null;
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
    } catch (error) {
      toast.error(getErrorMessage(error));
      console.error("Failed to update policy:", error);
    } finally {
      setActionState(null);
    }
  };

  const onUpdatePolicyStatus = async (configKey: string, active: boolean) => {
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
      toast.success(getToggleToastMessage(configKey, active));
    } catch (error) {
      toast.error(getErrorMessage(error));
      console.error("Failed to update policy status:", error);
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
    } catch (error) {
      toast.error(getErrorMessage(error));
      console.error("Failed to reset policy:", error);
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
          {policies.length === 0 ? (
            <div className="p-6 text-sm text-muted-foreground">No policies configured.</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Policy</TableHead>
                  <TableHead>Value</TableHead>
                  <TableHead>Constraints</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {policies.map((policy) => {
                  const isLoading = isPolicyLoading(policy.configKey);
                  const currentAction = getPolicyAction(policy.configKey);
                  const hasDefaultValue = policy.defaultValue !== null;
                  const currentValue = values[policy.configKey] ?? String(policy.configValue);
                  const hasChanges = currentValue !== String(policy.configValue);

                  return (
                    <TableRow key={policy.id} className={!policy.active ? "opacity-50" : undefined}>
                      <TableCell>
                        <div className="space-y-0.5">
                          <p className="font-mono text-sm font-medium">{policy.configKey}</p>
                          <p className="text-xs text-muted-foreground">{policy.configKeyLabel}</p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          value={currentValue}
                          onChange={(event) => {
                            setValues((prev) => ({
                              ...prev,
                              [policy.configKey]: event.target.value,
                            }));
                          }}
                          className="w-28 font-mono"
                        />
                      </TableCell>
                      <TableCell>
                        <div className="text-xs text-muted-foreground">
                          <span className="font-medium">Min:</span> {policy.minValue ?? "-"}
                          {" / "}
                          <span className="font-medium">Max:</span> {policy.maxValue ?? "-"}
                          {" / "}
                          <span className="font-medium">Default:</span> {policy.defaultValue ?? "-"}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={policy.active ? "success" : "secondary"}>
                          {policy.active ? "Active" : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={isLoading || !hasChanges}
                            onClick={() => void onUpdatePolicy(policy.configKey, currentValue)}
                          >
                            {currentAction === "save" ? getActionLabel("save") : "Save"}
                          </Button>
                          {hasDefaultValue && (
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              disabled={isLoading}
                              onClick={() => void onResetPolicy(policy.configKey)}
                            >
                              {currentAction === "reset" ? (
                                getActionLabel("reset")
                              ) : (
                                <RotateCcw className="h-4 w-4" />
                              )}
                            </Button>
                          )}
                          <Button
                            type="button"
                            size="sm"
                            variant={policy.active ? "destructive" : "default"}
                            disabled={isLoading}
                            onClick={() => void onUpdatePolicyStatus(policy.configKey, !policy.active)}
                          >
                            {currentAction === "toggle"
                              ? getActionLabel("toggle")
                              : getToggleButtonLabel(policy.active)}
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
