import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { formatLabel } from "@/lib/utils";
import type { LookupOption } from "@/lib/api";
import { Search } from "lucide-react";

type TicketListFiltersProps = Readonly<{
  searchTerm: string;
  onSearchChange: (value: string) => void;
  statusFilter: string;
  onStatusFilterChange: (value: string) => void;
  statusOptions: string[];
  priorityFilter: string;
  onPriorityFilterChange: (value: string) => void;
  priorityOptions: string[];
  queueFilter: string;
  onQueueFilterChange: (value: string) => void;
  queueOptions: LookupOption[];
}>;

export function TicketListFilters({
  searchTerm,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  statusOptions,
  priorityFilter,
  onPriorityFilterChange,
  priorityOptions,
  queueFilter,
  onQueueFilterChange,
  queueOptions,
}: TicketListFiltersProps) {
  return (
    <Card>
      <CardContent className="grid gap-3 p-4 md:grid-cols-2 lg:grid-cols-4">
        <div className="relative md:col-span-2">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchTerm}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search ticket no, subject, customer, agent"
            className="pl-9"
          />
        </div>
        <Select value={statusFilter} onValueChange={onStatusFilterChange}>
          <SelectTrigger>
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Statuses</SelectItem>
            {statusOptions.map((status) => (
              <SelectItem key={status} value={status}>
                {formatLabel(status)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={priorityFilter} onValueChange={onPriorityFilterChange}>
          <SelectTrigger>
            <SelectValue placeholder="Priority" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Priorities</SelectItem>
            {priorityOptions.map((priority) => (
              <SelectItem key={priority} value={priority}>
                {formatLabel(priority)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={queueFilter} onValueChange={onQueueFilterChange}>
          <SelectTrigger>
            <SelectValue placeholder="Queue" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Queues</SelectItem>
            {queueOptions.map((queueOption) => (
              <SelectItem key={queueOption.code} value={queueOption.code}>
                {queueOption.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </CardContent>
    </Card>
  );
}
