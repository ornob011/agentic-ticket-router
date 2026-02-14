import { api, type UserMe } from "../lib/api";

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
