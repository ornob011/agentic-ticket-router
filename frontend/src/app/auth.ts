import { api, type ProfileResponse, type ProfileUpdateRequest, type SignupOptionsResponse, type UserMe, type UserSettingsResponse, type UserSettingsUpdateRequest, type ChangePasswordRequest } from "@/lib/api";

export const getMe = async (): Promise<UserMe> => {
  const response = await api.get<UserMe>("/auth/me");
  return response.data;
};

export const login = async (
  username: string,
  password: string,
  rememberMe = true
) => {
  const response = await api.post<UserMe>(
    "/auth/login",
    { username, password, rememberMe }
  );
  return response.data;
};

export const logout = async () => {
  await api.post("/auth/logout");
};

export const getProfile = async (): Promise<ProfileResponse> => {
  const response = await api.get<ProfileResponse>("/auth/profile");
  return response.data;
};

export const updateProfile = async (
  payload: ProfileUpdateRequest
): Promise<ProfileResponse> => {
  const response = await api.put<ProfileResponse>("/auth/profile", payload);
  return response.data;
};

export const getSettings = async (): Promise<UserSettingsResponse> => {
  const response = await api.get<UserSettingsResponse>("/auth/settings");
  return response.data;
};

export const updateSettings = async (
  payload: UserSettingsUpdateRequest
): Promise<UserSettingsResponse> => {
  const response = await api.put<UserSettingsResponse>("/auth/settings", payload);
  return response.data;
};

export const changePassword = async (payload: ChangePasswordRequest): Promise<void> => {
  await api.post("/auth/change-password", payload);
};

export const getSignupOptions = async (): Promise<SignupOptionsResponse> => {
  const response = await api.get<SignupOptionsResponse>("/auth/signup-options");
  return response.data;
};
