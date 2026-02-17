import { apiGet } from "@/lib/api-loader";
import type { QueueMembershipInfo } from "@/lib/api";
import { endpoints } from "@/lib/endpoints";

export async function getQueueMemberships(): Promise<QueueMembershipInfo[]> {
  return apiGet<QueueMembershipInfo[]>(endpoints.admin.queueMemberships);
}
