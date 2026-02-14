import { api, type ProfileResponse, type ProfileUpdateRequest, type SignupOptionsResponse, type UserMe } from "../lib/api";

export const getMe = async (): Promise<UserMe> => {
  const response = await api.get<UserMe>("/auth/me");
  return response.data;
};

export const login = async (username: string, password: string) => {
  const response = await api.post<UserMe>("/auth/login", { username, password, rememberMe: true });
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

export const getSignupOptions = async (): Promise<SignupOptionsResponse> => {
  const response = await api.get<SignupOptionsResponse>("/auth/signup-options");
  return response.data;
};
