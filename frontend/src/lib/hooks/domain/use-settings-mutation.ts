import { useFormMutation } from "@/lib/hooks/use-api-mutation";
import { updateProfile, updateSettings, changePassword } from "@/app/auth";
import type { ProfileUpdateRequest, UserSettingsUpdateRequest, ChangePasswordRequest } from "@/lib/api";
import { parseApiError } from "@/lib/api-error";

export function useUpdateProfileMutation() {
  return useFormMutation({
    mutationFn: (payload: ProfileUpdateRequest) => updateProfile(payload),
    onSuccessMessage: "Profile updated",
    onErrorMessage: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.detail || "Failed to update profile";
    },
    mapFieldErrors: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.fieldErrors;
    },
    invalidateQueries: [["profile"], ["me"]],
    revalidate: false,
  });
}

export function useUpdateSettingsMutation() {
  return useFormMutation({
    mutationFn: (payload: UserSettingsUpdateRequest) => updateSettings(payload),
    onSuccessMessage: "Settings saved",
    onErrorMessage: "Failed to save settings",
    mapFieldErrors: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.fieldErrors;
    },
    invalidateQueries: [["settings"]],
    revalidate: false,
  });
}

export function useChangePasswordMutation() {
  return useFormMutation({
    mutationFn: (payload: ChangePasswordRequest) => changePassword(payload),
    onSuccessMessage: "Password changed successfully",
    onErrorMessage: (error) => {
      const parsedError = parseApiError(error);
      if (parsedError.fieldErrors.currentPassword) {
        return parsedError.fieldErrors.currentPassword;
      }
      return parsedError.detail || "Failed to change password";
    },
    mapFieldErrors: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.fieldErrors;
    },
    revalidate: false,
  });
}
