import type { AssignableAgentOption } from "@/lib/api";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type AgentSelectWithWorkloadProps = Readonly<{
  agents: AssignableAgentOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}>;

function getWorkloadBadgeVariant(openTickets: number): "success" | "warning" | "destructive" {
  if (openTickets === 0) return "success";
  if (openTickets <= 3) return "success";
  if (openTickets <= 6) return "warning";
  return "destructive";
}

function getWorkloadLabel(openTickets: number): string {
  if (openTickets === 0) return "Available";
  if (openTickets === 1) return "1 ticket";
  return `${openTickets} tickets`;
}

export function AgentSelectWithWorkload({
  agents,
  value,
  onChange,
  placeholder = "Select agent",
  disabled = false,
  className,
}: AgentSelectWithWorkloadProps) {
  return (
    <Select value={value} onValueChange={onChange} disabled={disabled}>
      <SelectTrigger className={className}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {agents.map((agent) => (
          <SelectItem key={agent.id} value={String(agent.id)}>
            <div className="flex items-center gap-2">
              <span>{agent.fullName || agent.username}</span>
              <Badge
                variant={getWorkloadBadgeVariant(agent.openTickets)}
                className="ml-1 text-[10px] px-1.5 py-0"
              >
                {getWorkloadLabel(agent.openTickets)}
              </Badge>
            </div>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export function AgentWorkloadBadge({ openTickets }: Readonly<{ openTickets: number }>) {
  return (
    <Badge
      variant={getWorkloadBadgeVariant(openTickets)}
      className={cn("text-[10px] px-1.5 py-0.5")}
    >
      {getWorkloadLabel(openTickets)}
    </Badge>
  );
}
