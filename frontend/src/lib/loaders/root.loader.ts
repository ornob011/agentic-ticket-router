import { apiGet } from "@/lib/api-loader";
import type { UserMe } from "@/lib/api";

export type RootLoaderData = {
  user: UserMe | null;
  isAuthenticated: boolean;
};

export async function rootLoader(): Promise<RootLoaderData> {
  try {
    const user = await apiGet<UserMe>("/auth/me");
    return { user, isAuthenticated: true };
  } catch {
    return { user: null, isAuthenticated: false };
  }
}
