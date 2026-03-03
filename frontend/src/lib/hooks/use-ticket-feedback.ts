import { useCallback, useEffect, useState } from "react";
import { getFeedbackForTicket } from "@/app/feedback";
import type { FeedbackResponse } from "@/lib/api";

export function useTicketFeedback(ticketId: number) {
  const [feedbackItems, setFeedbackItems] = useState<FeedbackResponse[]>([]);

  const reloadFeedback = useCallback(async () => {
    if (!Number.isFinite(ticketId) || ticketId <= 0) {
      setFeedbackItems([]);
      return;
    }

    try {
      const response = await getFeedbackForTicket(ticketId);
      setFeedbackItems(response);
    } catch {
      setFeedbackItems([]);
    }
  }, [ticketId]);

  useEffect(() => {
    void reloadFeedback();
  }, [reloadFeedback]);

  return { feedbackItems, reloadFeedback };
}
