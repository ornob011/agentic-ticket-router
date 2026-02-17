import { useMemo } from "react";
import { useMutation, useQueryClient, type UseMutationOptions } from "@tanstack/react-query";
import { useRevalidator } from "react-router-dom";
import { notification } from "@/lib/services/notification.service";
import { parseApiError, getErrorMessage, getFieldErrors, isApiError, type ApiError } from "@/lib/api-error";

type MutationContext = {
  previousData?: unknown;
};

type ApiMutationConfig<TData, TVariables, TError = ApiError> = Omit<
  UseMutationOptions<TData, TError, TVariables, MutationContext>,
  "mutationFn" | "onError" | "onSuccess"
> & {
  mutationFn: (variables: TVariables) => Promise<TData>;
  onSuccess?: (data: TData, variables: TVariables) => void | Promise<void>;
  onError?: (error: TError, variables: TVariables) => void | Promise<void>;
  onSuccessMessage?: string | ((data: TData, variables: TVariables) => string);
  onErrorMessage?: string | ((error: TError, variables: TVariables) => string);
  showNotification?: boolean;
  revalidate?: boolean;
  invalidateQueries?: (string | readonly unknown[])[];
};

export function useApiMutation<TData, TVariables, TError = ApiError>(
  config: ApiMutationConfig<TData, TVariables, TError>
) {
  const revalidator = useRevalidator();
  const queryClient = useQueryClient();
  const { 
    mutationFn, 
    onSuccess, 
    onError, 
    onSuccessMessage, 
    onErrorMessage,
    showNotification = true,
    revalidate = true,
    invalidateQueries,
    ...mutationOptions 
  } = config;

  return useMutation<TData, TError, TVariables, MutationContext>({
    ...mutationOptions,
    mutationFn,
    onSuccess: async (data, variables, onMutateResult, context) => {
      if (showNotification && onSuccessMessage) {
        const message = typeof onSuccessMessage === "function"
          ? onSuccessMessage(data, variables)
          : onSuccessMessage;
        notification.success(message);
      }

      if (invalidateQueries) {
        await Promise.all(
          invalidateQueries.map(queryKey => 
            queryClient.invalidateQueries({ queryKey: typeof queryKey === 'string' ? [queryKey] : queryKey })
          )
        );
      }

      if (revalidate) {
        await revalidator.revalidate();
      }

      await onSuccess?.(data, variables);
      
      mutationOptions.onSettled?.(data, null, variables, onMutateResult, context);
    },
    onError: async (error, variables, onMutateResult, context) => {
      const apiError = parseApiError(error) as TError;
      
      if (showNotification) {
        const message = onErrorMessage
          ? typeof onErrorMessage === "function"
            ? onErrorMessage(apiError, variables)
            : onErrorMessage
          : getErrorMessage(error);
        notification.error(message);
      }

      await onError?.(apiError, variables);
      
      mutationOptions.onSettled?.(undefined, error, variables, onMutateResult, context);
    },
  });
}

type FormMutationConfig<TData, TVariables, TError = ApiError> = ApiMutationConfig<TData, TVariables, TError> & {
  mapFieldErrors?: (error: TError) => Record<string, string>;
};

export function useFormMutation<TData, TVariables, TError = ApiError>(
  config: FormMutationConfig<TData, TVariables, TError>
) {
  const { mapFieldErrors, ...mutationConfig } = config;

  const mutation = useApiMutation<TData, TVariables, TError>(mutationConfig);

  const fieldErrors = useMemo(() => {
    if (!mutation.error) return {};
    if (mapFieldErrors) return mapFieldErrors(mutation.error);
    if (isApiError(mutation.error)) return getFieldErrors(mutation.error);
    return {};
  }, [mutation.error, mapFieldErrors]);

  const globalError = useMemo(() => {
    if (!mutation.error) return null;
    if (isApiError(mutation.error)) {
      const apiError = mutation.error;
      if (apiError.globalErrors.length > 0) return apiError.globalErrors[0];
      if (Object.keys(fieldErrors).length === 0) return apiError.detail;
    }
    return getErrorMessage(mutation.error);
  }, [mutation.error, fieldErrors]);

  return {
    ...mutation,
    fieldErrors,
    globalError,
    hasFieldErrors: Object.keys(fieldErrors).length > 0,
    getFieldError: (field: string) => fieldErrors[field] || null,
  };
}
