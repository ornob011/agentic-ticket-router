import { useState, useCallback, useRef, useEffect } from "react";
import { endpoints } from "@/lib/endpoints";

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
    es.onmessage = (e) => setDraft((prev) => prev + e.data);
    es.addEventListener("done", () => {
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
