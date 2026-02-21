import { useApiMutation } from "@/lib/hooks/use-api-mutation";
import { api } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";
import type { FeedbackRequest, FeedbackResponse } from "@/lib/api";

export function useSubmitFeedbackMutation() {
  return useApiMutation({
    mutationFn: async (request: FeedbackRequest) => {
      const response = await api.post<FeedbackResponse>(endpoints.feedback.submit, request);
      return response.data;
    },
    onSuccessMessage: (_, vars) => {
      const typeLabels: Record<string, string> = {
        RATING: "Rating submitted",
        CORRECTION: "Correction recorded",
        REJECTION: "Rejection recorded",
        APPROVAL: "Approval recorded",
      };
      return typeLabels[vars.feedbackType] || "Feedback submitted";
    },
    onErrorMessage: "Failed to submit feedback",
    revalidate: true,
  });
}

export function useSubmitRatingMutation() {
  return useApiMutation({
    mutationFn: async (variables: { ticketId: number; rating: number; notes?: string }) => {
      const request: FeedbackRequest = {
        ticketId: variables.ticketId,
        feedbackType: "RATING",
        rating: variables.rating,
        notes: variables.notes,
      };
      const response = await api.post<FeedbackResponse>(endpoints.feedback.submit, request);
      return response.data;
    },
    onSuccessMessage: (_, vars) => `Rating submitted: ${vars.rating}/5`,
    onErrorMessage: "Failed to submit rating",
    revalidate: true,
  });
}

export function useSubmitCorrectionMutation() {
  return useApiMutation({
    mutationFn: async (variables: {
      ticketId: number;
      originalCategory?: string;
      correctedCategory?: string;
      originalAction: string;
      correctedAction: string;
      notes?: string;
    }) => {
      const request: FeedbackRequest = {
        ticketId: variables.ticketId,
        feedbackType: "CORRECTION",
        originalCategory: variables.originalCategory,
        correctedCategory: variables.correctedCategory,
        originalAction: variables.originalAction,
        correctedAction: variables.correctedAction,
        notes: variables.notes,
      };
      const response = await api.post<FeedbackResponse>(endpoints.feedback.submit, request);
      return response.data;
    },
    onSuccessMessage: "Correction recorded for learning",
    onErrorMessage: "Failed to submit correction",
    revalidate: true,
  });
}
