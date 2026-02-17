import { useLoaderData, useRevalidator } from "react-router-dom";
import { useEffect, useState } from "react";
import type { AdminPoliciesLoaderData } from "@/router";
import { api } from "@/lib/api";
import { getErrorMessage, parseApiError } from "@/lib/api-error";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Settings, Loader2, MoreVertical } from "lucide-react";
import { toast } from "sonner";

export default function AdminPoliciesPage() {
  const data = useLoaderData<AdminPoliciesLoaderData>();
  const revalidator = useRevalidator();
  const policies = data ?? [];
  const [values, setValues] = useState<Record<string, string>>({});
  const [valueErrors, setValueErrors] = useState<Record<string, string>>({});
  const [loadingKey, setLoadingKey] = useState<string | null>(null);

  useEffect(() => {
    setValues(
      Object.fromEntries(
        policies.map((policy) => [policy.configKey, String(policy.configValue)])
      )
    );
    setValueErrors({});
  }, [policies]);

  const onSave = async (configKey: string, value: string) => {
    const configValue = Number(value);
    if (Number.isNaN(configValue)) {
      setValueErrors((prev) => ({ ...prev, [configKey]: "Value must be a number" }));
      return;
    }

    setValueErrors((prev) => ({ ...prev, [configKey]: "" }));
    setLoadingKey(configKey);
    try {
      await api.patch("/admin/policy-config", { configKey, configValue });
      await revalidator.revalidate();
      toast.success(`${configKey} updated`);
    } catch (error) {
      const apiError = parseApiError(error);
      const fieldError = apiError.fieldErrors.configValue || apiError.globalErrors[0];

      if (fieldError) {
        setValueErrors((prev) => ({ ...prev, [configKey]: fieldError }));
        return;
      }

      toast.error(apiError.detail);
    } finally {
      setLoadingKey(null);
    }
  };

  const onToggleStatus = async (configKey: string, active: boolean) => {
    setLoadingKey(configKey);
    try {
      await api.patch("/admin/policy-config/status", { configKey, active });
      await revalidator.revalidate();
      toast.success(`${configKey} ${active ? "activated" : "deactivated"}`);
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const onReset = async (configKey: string) => {
    setLoadingKey(configKey);
    try {
      await api.post(`/admin/policy-config/${configKey}/reset`);
      await revalidator.revalidate();
      toast.success(`${configKey} reset to default`);
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
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
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Policy</TableHead>
                    <TableHead className="min-w-[80px]">Value</TableHead>
                    <TableHead className="text-center hidden sm:table-cell">Status</TableHead>
                    <TableHead className="text-right w-[50px]">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {policies.map((policy) => {
                    const isLoading = loadingKey === policy.configKey;
                    const currentValue = values[policy.configKey] ?? String(policy.configValue);
                    const valueError = valueErrors[policy.configKey];
                    const hasChanges = currentValue !== String(policy.configValue);

                    return (
                      <TableRow
                        key={policy.id}
                        className={`${!policy.active ? "opacity-50" : ""} ${hasChanges ? "bg-amber-50/50" : ""}`}
                      >
                        <TableCell className="py-3">
                          <div className="flex flex-col gap-0.5">
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-sm font-medium truncate">{policy.configKey}</span>
                              {hasChanges && <span className="text-amber-600 text-xs shrink-0">*</span>}
                              <span className="sm:hidden shrink-0">
                                {policy.active ? (
                                  <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" />
                                ) : (
                                  <span className="w-2 h-2 rounded-full bg-gray-300 inline-block" />
                                )}
                              </span>
                            </div>
                            <p className="text-xs text-muted-foreground truncate">{policy.configKeyLabel}</p>
                            <p className="text-xs text-muted-foreground/50 hidden md:block">
                              Min {policy.minValue ?? "-"}  Max {policy.maxValue ?? "-"}  Default {policy.defaultValue ?? "-"}
                            </p>
                          </div>
                        </TableCell>
                        <TableCell className="py-3 min-w-[80px]">
                          <div className="space-y-1">
                            <Input
                              type="number"
                              step="any"
                              value={currentValue}
                              onChange={(e) => {
                                const nextValue = e.target.value;
                                setValues((prev) => ({ ...prev, [policy.configKey]: nextValue }));
                                if (valueErrors[policy.configKey]) {
                                  setValueErrors((prev) => ({ ...prev, [policy.configKey]: "" }));
                                }
                              }}
                              className={`font-mono text-sm ${hasChanges ? "border-amber-300" : ""} ${valueError ? "border-destructive" : ""}`}
                              disabled={isLoading}
                            />
                            {valueError && <p className="text-xs text-destructive">{valueError}</p>}
                          </div>
                        </TableCell>
                        <TableCell className="text-center hidden sm:table-cell py-3">
                          <Badge variant={policy.active ? "success" : "secondary"} className="text-xs">
                            {policy.active ? "Active" : "Inactive"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right py-3 w-[50px]">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                disabled={isLoading}
                              >
                                {isLoading ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <MoreVertical className="h-4 w-4" />
                                )}
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                disabled={!hasChanges}
                                onClick={() => void onSave(policy.configKey, currentValue)}
                              >
                                Save Changes
                              </DropdownMenuItem>
                              {policy.defaultValue !== null && (
                                <>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem onClick={() => void onReset(policy.configKey)}>
                                    Reset to Default ({policy.defaultValue})
                                  </DropdownMenuItem>
                                </>
                              )}
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                className={policy.active ? "text-destructive focus:text-destructive" : "text-emerald-600 focus:text-emerald-600"}
                                onClick={() => void onToggleStatus(policy.configKey, !policy.active)}
                              >
                                {policy.active ? "Deactivate" : "Activate"}
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
