import { useLoaderData } from "react-router-dom";
import type { AuditLogLoaderData } from "@/router";
import { formatDateTime, formatLabel } from "@/lib/utils";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { FileText } from "lucide-react";

function getEventTone(eventType: string): "default" | "success" | "warning" | "destructive" | "secondary" {
  if (eventType.includes("CREATE") || eventType.includes("RESOLVE")) return "success";
  if (eventType.includes("DELETE") || eventType.includes("ESCALATE")) return "destructive";
  if (eventType.includes("UPDATE") || eventType.includes("ASSIGN")) return "default";
  return "secondary";
}

export default function AuditLogPage() {
  const data = useLoaderData<AuditLogLoaderData>();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Audit Log</h1>
        <p className="text-muted-foreground">Chronological record of system events and user actions</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Event History
          </CardTitle>
          <CardDescription>{data?.totalElements ?? 0} recorded events</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {data && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Event Type</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="hidden md:table-cell">Performed By</TableHead>
                  <TableHead className="hidden lg:table-cell text-right">Timestamp</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((event) => (
                  <TableRow key={event.id}>
                    <TableCell>
                      <Badge variant={getEventTone(event.eventType)}>
                        {event.eventTypeLabel || formatLabel(event.eventType)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <p className="line-clamp-2">{event.description}</p>
                    </TableCell>
                    <TableCell className="hidden md:table-cell text-muted-foreground">
                      {event.performedBy}
                    </TableCell>
                    <TableCell className="hidden lg:table-cell text-right text-sm text-muted-foreground">
                      {formatDateTime(event.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
