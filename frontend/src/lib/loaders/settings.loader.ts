import { apiGet } from "@/lib/api-loader";
import { endpoints } from "@/lib/endpoints";
import type { ProfileResponse, UserSettingsResponse, SignupOptionsResponse } from "@/lib/api";

export type SettingsLoaderData = {
  profile: ProfileResponse;
  settings: UserSettingsResponse;
  signupOptions: SignupOptionsResponse | null;
};

export async function settingsLoader(): Promise<SettingsLoaderData> {
  const [profile, settings] = await Promise.all([
    apiGet<ProfileResponse>(endpoints.auth.profile),
    apiGet<UserSettingsResponse>(endpoints.auth.settings),
  ]);

  let signupOptions: SignupOptionsResponse | null = null;
  try {
    signupOptions = await apiGet<SignupOptionsResponse>(endpoints.auth.signupOptions);
  } catch {
    // Signup options are optional, mainly for customer profiles
  }

  return { profile, settings, signupOptions };
}
