import { apiGet } from "@/lib/api-loader";
import type { UserInfo } from "@/lib/api";

export type AdminUsersLoaderData = UserInfo[];

export async function adminUsersLoader(): Promise<AdminUsersLoaderData> {
  return apiGet<UserInfo[]>("/admin/users");
}
