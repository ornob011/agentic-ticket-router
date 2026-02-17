import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { UserInfo } from "@/lib/api";

export type AdminUsersLoaderData = UserInfo[];

export async function adminUsersLoader(): Promise<AdminUsersLoaderData> {
  return apiGet<UserInfo[]>(endpoints.admin.users);
}
