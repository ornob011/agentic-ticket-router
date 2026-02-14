import { api } from "@/lib/api";
import type { ApiError } from "@/lib/api-error";
import { parseApiError } from "@/lib/api-error";

export async function apiGet<T>(url: string): Promise<T> {
  const response = await api.get<T>(url);
  return response.data;
}

export function throwApiError(error: unknown): never {
  const apiError = parseApiError(error);
  throw apiError;
}

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    "code" in error
  );
}

export type { ApiError };
