import { useApiMutation, useFormMutation } from "@/lib/hooks/use-api-mutation";
import { api } from "@/lib/api";
import type { UserRole } from "@/lib/api";
import { parseApiError } from "@/lib/api-error";
import { endpoints } from "@/lib/endpoints";

type CreateUserVariables = {
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
  password: string;
};

type CreateMembershipVariables = {
  userId: number;
  queue: string;
};

type UpdatePolicyVariables = {
  configKey: string;
  configValue: number;
};

type TogglePolicyStatusVariables = {
  configKey: string;
  active: boolean;
};

type ActivateModelVariables = {
  modelTag: string;
};

export function useCreateUserMutation() {
  return useApiMutation({
    mutationFn: (variables: CreateUserVariables) => api.post(endpoints.admin.users, variables),
    onSuccessMessage: "Staff user created",
    onErrorMessage: "Failed to create user",
    revalidate: true,
  });
}

export function useCreateMembershipMutation() {
  return useApiMutation({
    mutationFn: (variables: CreateMembershipVariables) =>
      api.post(endpoints.admin.queueMemberships, variables),
    onSuccessMessage: "Queue membership added",
    onErrorMessage: "Failed to add membership",
    revalidate: true,
  });
}

export function useDeleteMembershipMutation() {
  return useApiMutation({
    mutationFn: (membershipId: number) =>
      api.delete(endpoints.admin.queueMembership(membershipId)),
    onSuccessMessage: "Queue membership removed",
    onErrorMessage: "Failed to remove membership",
    revalidate: true,
  });
}

export function useUpdatePolicyMutation() {
  return useFormMutation({
    mutationFn: (variables: UpdatePolicyVariables) =>
      api.patch(endpoints.admin.policyConfig, variables),
    onSuccessMessage: (_, vars) => `${vars.configKey} updated`,
    onErrorMessage: "Failed to update policy",
    mapFieldErrors: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.fieldErrors;
    },
    revalidate: true,
  });
}

export function useTogglePolicyStatusMutation() {
  return useApiMutation({
    mutationFn: (variables: TogglePolicyStatusVariables) =>
      api.patch(endpoints.admin.policyStatus, variables),
    onSuccessMessage: (_, vars) => `${vars.configKey} ${vars.active ? "activated" : "deactivated"}`,
    onErrorMessage: "Failed to toggle policy status",
    revalidate: true,
  });
}

export function useResetPolicyMutation() {
  return useApiMutation({
    mutationFn: (configKey: string) =>
      api.post(endpoints.admin.resetPolicy(configKey)),
    onSuccessMessage: (_, configKey) => `${configKey} reset to default`,
    onErrorMessage: "Failed to reset policy",
    revalidate: true,
  });
}

export function useActivateModelMutation() {
  return useApiMutation({
    mutationFn: (variables: ActivateModelVariables) =>
      api.post(endpoints.admin.activateModel, { modelTag: variables.modelTag }),
    onSuccessMessage: (_, vars) => `Model ${vars.modelTag} activated successfully`,
    onErrorMessage: "Failed to activate model",
    revalidate: true,
  });
}
