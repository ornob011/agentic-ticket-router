import { api, type FeedbackRequest, type FeedbackResponse, type FeedbackSummary } from "@/lib/api";
import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";

export async function submitFeedback(request: FeedbackRequest): Promise<FeedbackResponse> {
  const response = await api.post<FeedbackResponse>(endpoints.feedback.submit, request);
  return response.data;
}

export async function getFeedbackForTicket(ticketId: number): Promise<FeedbackResponse[]> {
  return apiGet<FeedbackResponse[]>(endpoints.feedback.listByTicket(ticketId));
}

export async function getFeedbackSummary(ticketId: number): Promise<FeedbackSummary> {
  return apiGet<FeedbackSummary>(endpoints.feedback.summary(ticketId));
}
