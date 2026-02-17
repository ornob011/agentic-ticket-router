import { useEffect, useState } from "react";
import { getTicketMetadata } from "@/app/tickets";
import type { LookupOption } from "@/lib/api";

export function useQueueMetadataOptions() {
  const [queueMetadataOptions, setQueueMetadataOptions] = useState<LookupOption[]>([]);

  useEffect(() => {
    const loadQueueOptions = async () => {
      try {
        const metadata = await getTicketMetadata();
        setQueueMetadataOptions(metadata.queues ?? []);
      } catch {
        setQueueMetadataOptions([]);
      }
    };

    void loadQueueOptions();
  }, []);

  return queueMetadataOptions;
}
