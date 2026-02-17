import type { AuditEventItem } from "@/lib/api";
import { formatDateTime, formatLabel } from "@/lib/utils";
import { PageHeader } from "@/components/ui/page-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EmptyState } from "@/components/ui/empty-state";
import { FileText, Clock } from "lucide-react";

function getEventTone(eventType: string): "default" | "success" | "warning" | "destructive" | "secondary" {
  if (eventType.includes("CREATE") || eventType.includes("RESOLVE")) return "success";
  if (eventType.includes("DELETE") || eventType.includes("ESCALATE")) return "destructive";
  if (eventType.includes("UPDATE") || eventType.includes("ASSIGN")) return "default";
  return "secondary";
}

function AuditEventRow({ event }: Readonly<{ event: AuditEventItem }>) {
  return (
    <TableRow>
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
  );
}

type AuditLogScreenProps = Readonly<{
  events: AuditEventItem[];
  totalElements: number;
}>;

export function AuditLogScreen({ events, totalElements }: AuditLogScreenProps) {
  const hasEvents = events.length > 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit Log"
        description="Chronological record of system events and user actions"
      />

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Event History
          </CardTitle>
          <CardDescription>{totalElements} recorded events</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {hasEvents ? (
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
                {events.map((event) => (
                  <AuditEventRow key={event.id} event={event} />
                ))}
              </TableBody>
            </Table>
          ) : (
            <EmptyState
              icon={Clock}
              title="No audit events"
              description="System audit entries will appear here as actions are performed."
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
