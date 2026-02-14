import { useLoaderData, useRevalidator } from "react-router-dom";
import { useState } from "react";
import { formatDateTime } from "@/lib/utils";
import { api } from "@/lib/api";
import type { AdminModelsLoaderData } from "@/router";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Brain, Check, Zap } from "lucide-react";
import { toast } from "sonner";

export default function AdminModelsPage() {
  const data = useLoaderData<AdminModelsLoaderData>();
  const revalidator = useRevalidator();
  const [activating, setActivating] = useState<string | null>(null);

  const models = data ?? [];

  const activateModel = async (modelTag: string) => {
    setActivating(modelTag);
    try {
      await api.post("/admin/model-registry/activate", { modelTag });
      await revalidator.revalidate();
      toast.success(`Model ${modelTag} activated successfully`);
    } catch (error) {
      toast.error("Failed to activate model");
      console.error("Failed to activate model:", error);
    } finally {
      setActivating(null);
    }
  };

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
                  let activatedAtLabel = "-";
                  if (model.activatedAt) {
                    activatedAtLabel = formatDateTime(model.activatedAt);
                  }

                  const isActivating = activating === model.modelTag;
                  let activateButtonLabel = "Activate";
                  if (isActivating) {
                    activateButtonLabel = "Activating...";
                  }

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
                        {model.active && (
                          <Badge variant="success">
                            <Check className="mr-1 h-3 w-3" />
                            Active
                          </Badge>
                        )}
                        {!model.active && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => activateModel(model.modelTag)}
                            disabled={isActivating}
                          >
                            {activateButtonLabel}
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
