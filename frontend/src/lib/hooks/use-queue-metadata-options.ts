import { useEffect, useState } from "react";
import { api, type LookupOption } from "@/lib/api";

export function useQueueMetadataOptions() {
  const [queueMetadataOptions, setQueueMetadataOptions] = useState<LookupOption[]>([]);

  useEffect(() => {
    const loadQueueOptions = async () => {
      try {
        const response = await api.get<{ queues: LookupOption[] }>("/tickets/meta");
        setQueueMetadataOptions(response.data.queues ?? []);
      } catch {
        setQueueMetadataOptions([]);
      }
    };

    void loadQueueOptions();
  }, []);

  return queueMetadataOptions;
}
