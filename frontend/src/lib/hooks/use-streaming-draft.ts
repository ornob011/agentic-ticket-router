import { useState, useCallback, useRef, useEffect } from "react";
import { endpoints } from "@/lib/endpoints";
import { SSE_EVENT_NAME, SSE_EVENT_TYPE, type SseEventEnvelope } from "@/lib/sse-contract";

type DraftTokenPayload = {
  token: string;
};

export function useStreamingDraft(ticketId: number) {
  const [draft, setDraft] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const esRef = useRef<EventSource | null>(null);

  const start = useCallback(() => {
    esRef.current?.close();
    setDraft("");
    setIsStreaming(true);

    const es = new EventSource(`/api/v1${endpoints.tickets.draftStream(ticketId)}`, { withCredentials: true });
    esRef.current = es;

    es.addEventListener(SSE_EVENT_NAME.EVENT, (e) => {
      const envelope = JSON.parse(e.data) as SseEventEnvelope<DraftTokenPayload>;
      if (envelope.eventType !== SSE_EVENT_TYPE.TOKEN) {
        return;
      }

      const token = envelope.payload?.token;
      if (!token) {
        return;
      }

      setDraft((prev) => prev + token);
    });

    es.addEventListener(SSE_EVENT_NAME.COMPLETE, () => {
      setIsStreaming(false);
      es.close();
    });

    es.addEventListener(SSE_EVENT_NAME.ERROR, () => {
      setIsStreaming(false);
      es.close();
    });

    es.onerror = () => {
      setIsStreaming(false);
      es.close();
    };
  }, [ticketId]);

  const clear = useCallback(() => setDraft(""), []);

  useEffect(() => () => esRef.current?.close(), []);

  return { draft, isStreaming, start, clear };
}
