import type { PolicyInfo } from "@/lib/api";
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

type AdminPoliciesScreenProps = Readonly<{
  policies: PolicyInfo[];
  values: Record<string, string>;
  valueErrors: Record<string, string>;
  loadingKey: string | null;
  onValueChange: (configKey: string, value: string) => void;
  onSave: (configKey: string, value: string) => Promise<void>;
  onToggleStatus: (configKey: string, active: boolean) => Promise<void>;
  onReset: (configKey: string) => Promise<void>;
}>;

export function AdminPoliciesScreen({
  policies,
  values,
  valueErrors,
  loadingKey,
  onValueChange,
  onSave,
  onToggleStatus,
  onReset,
}: AdminPoliciesScreenProps) {
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
                              onChange={(event) => onValueChange(policy.configKey, event.target.value)}
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
                              <Button variant="ghost" size="icon" disabled={isLoading}>
                                {isLoading ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <MoreVertical className="h-4 w-4" />
                                )}
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem disabled={!hasChanges} onClick={() => void onSave(policy.configKey, currentValue)}>
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
