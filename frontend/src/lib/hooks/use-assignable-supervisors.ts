import { useCallback, useEffect, useState } from "react";
import { getAssignableSupervisors } from "@/app/escalations";
import type { AssignableSupervisorOption } from "@/lib/api";

export function useAssignableSupervisors(enabled: boolean) {
  const [assignableSupervisors, setAssignableSupervisors] = useState<AssignableSupervisorOption[]>([]);

  const reloadAssignableSupervisors = useCallback(async () => {
    if (!enabled) {
      setAssignableSupervisors([]);
      return;
    }

    try {
      const supervisors = await getAssignableSupervisors();
      setAssignableSupervisors(supervisors);
    } catch {
      setAssignableSupervisors([]);
    }
  }, [enabled]);

  useEffect(() => {
    void reloadAssignableSupervisors();
  }, [reloadAssignableSupervisors]);

  return { assignableSupervisors, reloadAssignableSupervisors };
}
