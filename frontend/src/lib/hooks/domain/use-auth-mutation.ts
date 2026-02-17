import { useApiMutation, useFormMutation } from "@/lib/hooks/use-api-mutation";
import { login, logout } from "@/app/auth";
import { api } from "@/lib/api";
import { parseApiError } from "@/lib/api-error";
import { endpoints } from "@/lib/endpoints";

type LoginVariables = {
  username: string;
  password: string;
  rememberMe: boolean;
};

type SignupVariables = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  fullName: string;
  companyName: string;
  phoneNumber: string;
  address: string;
  city: string;
  countryIso2: string;
  customerTierCode: string;
  preferredLanguageCode: string;
};

export function useLoginMutation() {
  return useFormMutation({
    mutationFn: (variables: LoginVariables) =>
      login(variables.username, variables.password, variables.rememberMe),
    onErrorMessage: (error) => {
      const parsedError = parseApiError(error);
      if (parsedError.status === 401 || parsedError.status === 403) {
        return "Invalid username or password. Please try again.";
      }
      return parsedError.detail || "Unable to sign in right now. Please try again.";
    },
    revalidate: false,
  });
}

export function useSignupMutation() {
  return useFormMutation({
    mutationFn: (variables: SignupVariables) => api.post(endpoints.auth.signup, variables),
    onSuccessMessage: "Account created successfully!",
    onErrorMessage: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.detail || "Signup failed. Please try again.";
    },
    mapFieldErrors: (error) => {
      const parsedError = parseApiError(error);
      return parsedError.fieldErrors;
    },
    revalidate: false,
  });
}

export function useLogoutMutation() {
  return useApiMutation({
    mutationFn: () => logout(),
    onSuccessMessage: "You have been signed out",
    revalidate: false,
  });
}
