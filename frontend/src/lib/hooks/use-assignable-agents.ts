import { useCallback, useEffect, useState } from "react";
import { getAssignableAgents } from "@/app/tickets";
import type { AssignableAgentOption } from "@/lib/api";

export function useAssignableAgents(enabled: boolean) {
  const [assignableAgents, setAssignableAgents] = useState<AssignableAgentOption[]>([]);

  const reloadAssignableAgents = useCallback(async () => {
    if (!enabled) {
      setAssignableAgents([]);
      return;
    }

    try {
      const agents = await getAssignableAgents();
      setAssignableAgents(agents);
    } catch {
      setAssignableAgents([]);
    }
  }, [enabled]);

  useEffect(() => {
    void reloadAssignableAgents();
  }, [reloadAssignableAgents]);

  return { assignableAgents, reloadAssignableAgents };
}
