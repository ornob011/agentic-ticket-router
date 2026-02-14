import { apiGet } from "@/lib/api-loader";
import type { ProfileResponse, UserSettingsResponse, SignupOptionsResponse } from "@/lib/api";

export type SettingsLoaderData = {
  profile: ProfileResponse;
  settings: UserSettingsResponse;
  signupOptions: SignupOptionsResponse | null;
};

export async function settingsLoader(): Promise<SettingsLoaderData> {
  const [profile, settings] = await Promise.all([
    apiGet<ProfileResponse>("/auth/profile"),
    apiGet<UserSettingsResponse>("/auth/settings"),
  ]);

  let signupOptions: SignupOptionsResponse | null = null;
  try {
    signupOptions = await apiGet<SignupOptionsResponse>("/auth/signup-options");
  } catch {
    // Signup options are optional, mainly for customer profiles
  }

  return { profile, settings, signupOptions };
}
