import type { AxiosError, AxiosInstance } from "axios";
import { notification } from "@/lib/services/notification.service";

export type ApiError = {
  status: number;
  code: string;
  title: string;
  detail: string;
  type: string;
  instance: string;
  timestamp: string;
  fieldErrors: Record<string, string>;
  globalErrors: string[];
};

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    "code" in error &&
    "detail" in error
  );
}

export function parseApiError(error: unknown): ApiError {
  if (!error) {
    return createUnknownError();
  }

  if (isApiError(error)) {
    return error;
  }

  if (isAxiosError(error)) {
    return parseAxiosError(error);
  }

  if (error instanceof Error) {
    return {
      status: 0,
      code: "CLIENT_ERROR",
      title: "Error",
      detail: error.message,
      type: "/errors/client-error",
      instance: "",
      timestamp: new Date().toISOString(),
      fieldErrors: {},
      globalErrors: [],
    };
  }

  return createUnknownError();
}

function createUnknownError(): ApiError {
  return {
    status: 0,
    code: "UNKNOWN_ERROR",
    title: "Unknown Error",
    detail: "An unexpected error occurred. Please try again.",
    type: "/errors/unknown",
    instance: "",
    timestamp: new Date().toISOString(),
    fieldErrors: {},
    globalErrors: [],
  };
}

function isAxiosError(error: unknown): error is AxiosError {
  return (typeof error === "object" &&
      error !== null &&
      "isAxiosError" in error && (error as AxiosError).isAxiosError);
}

function parseAxiosError(error: AxiosError): ApiError {
  const response = error.response;

  if (!response) {
    return {
      status: 0,
      code: "NETWORK_ERROR",
      title: "Network Error",
      detail: "Unable to connect to the server. Please check your connection.",
      type: "/errors/network",
      instance: "",
      timestamp: new Date().toISOString(),
      fieldErrors: {},
      globalErrors: [],
    };
  }

  const data = response.data as Record<string, unknown> | undefined;

  return {
    status: response.status,
    code: (data?.code as string) || getErrorCodeFromStatus(response.status),
    title: (data?.title as string) || response.statusText,
    detail: (data?.detail as string) || "An error occurred.",
    type: (data?.type as string) || `/errors/${response.status}`,
    instance: (data?.instance as string) || "",
    timestamp: (data?.timestamp as string) || new Date().toISOString(),
    fieldErrors: (data?.fieldErrors as Record<string, string>) || {},
    globalErrors: (data?.globalErrors as string[]) || [],
  };
}

function getErrorCodeFromStatus(status: number): string {
  const statusMap: Record<number, string> = {
    400: "VALIDATION_ERROR",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "RESOURCE_NOT_FOUND",
    405: "METHOD_NOT_ALLOWED",
    409: "CONFLICT",
    422: "BUSINESS_RULE_VIOLATION",
    500: "INTERNAL_ERROR",
    502: "BAD_GATEWAY",
    503: "SERVICE_UNAVAILABLE",
  };
  return statusMap[status] || "UNKNOWN_ERROR";
}

export function isNetworkError(error: unknown): boolean {
  return isApiError(error) && error.code === "NETWORK_ERROR";
}

export function isUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.status === 401;
}

export function isForbiddenError(error: unknown): boolean {
  return isApiError(error) && error.status === 403;
}

export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.status === 404;
}

export function isValidationError(error: unknown): boolean {
  return isApiError(error) && (error.status === 400 || error.status === 422);
}

export function isServerError(error: unknown): boolean {
  return isApiError(error) && error.status >= 500;
}

export function getFieldError(error: unknown, fieldName: string): string | undefined {
  if (!isApiError(error)) return undefined;
  return error.fieldErrors[fieldName];
}

export function getFieldErrors(error: unknown): Record<string, string> {
  if (!isApiError(error)) return {};
  return error.fieldErrors;
}

export function getErrorMessage(error: unknown): string {
  const apiError = parseApiError(error);

  if (Object.keys(apiError.fieldErrors).length > 0) {
    return Object.values(apiError.fieldErrors)[0];
  }

  if (apiError.globalErrors.length > 0) {
    return apiError.globalErrors[0];
  }

  return apiError.detail;
}

export function showApiError(error: unknown, fallbackMessage = "An error occurred"): void {
  const apiError = parseApiError(error);
  notification.error(apiError.detail || fallbackMessage);
}

export function setupApiInterceptors(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      const apiError = parseApiError(error);

      if (apiError.status === 401) {
        return Promise.reject(apiError);
      }

      if (isServerError(apiError)) {
        notification.error("Server error. Please try again later.");
      }

      return Promise.reject(apiError);
    }
  );
}
