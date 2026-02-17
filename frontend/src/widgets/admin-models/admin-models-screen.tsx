import type { MouseEvent } from "react";
import type { ModelInfo } from "@/lib/api";
import { formatDateTime } from "@/lib/utils";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Brain, Check, Zap } from "lucide-react";

type AdminModelsScreenProps = Readonly<{
  models: ModelInfo[];
  activatingModelTag: string | undefined;
  onActivate: (modelTag: string) => Promise<void>;
}>;

export function AdminModelsScreen({ models, activatingModelTag, onActivate }: AdminModelsScreenProps) {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Model Registry</h1>
        <p className="text-muted-foreground">Configure and activate AI models for ticket routing</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Brain className="h-5 w-5" />
            Available Models
          </CardTitle>
          <CardDescription>LLM providers configured for the routing engine</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {models.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Model</TableHead>
                  <TableHead className="hidden md:table-cell">Provider</TableHead>
                  <TableHead className="hidden lg:table-cell">Activated At</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {models.map((model) => {
                  const activatedAtLabel = model.activatedAt ? formatDateTime(model.activatedAt) : "-";
                  const isActivating = activatingModelTag === model.modelTag;

                  return (
                    <TableRow key={model.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Zap className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{model.modelTag}</span>
                        </div>
                      </TableCell>
                      <TableCell className="hidden md:table-cell text-muted-foreground">
                        {model.provider}
                      </TableCell>
                      <TableCell className="hidden lg:table-cell text-sm text-muted-foreground">
                        {activatedAtLabel}
                      </TableCell>
                      <TableCell className="text-right">
                        {model.active ? (
                          <Badge variant="success">
                            <Check className="mr-1 h-3 w-3" />
                            Active
                          </Badge>
                        ) : (
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={(event: MouseEvent<HTMLButtonElement>) => {
                              event.preventDefault();
                              void onActivate(model.modelTag);
                            }}
                            disabled={isActivating}
                          >
                            {isActivating ? "Activating..." : "Activate"}
                          </Button>
                        )}
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
