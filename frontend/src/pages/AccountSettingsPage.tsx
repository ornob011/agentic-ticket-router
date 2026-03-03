import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useLoaderData } from "react-router-dom";
import { getProfile, getSettings, getSignupOptions } from "@/app/auth";
import { useUpdateProfileMutation, useUpdateSettingsMutation, useChangePasswordMutation } from "@/lib/hooks";
import { Skeleton } from "@/components/ui/skeleton";
import type {
  ProfileResponse,
  ProfileUpdateRequest,
  UserRole,
  UserSettingsUpdateRequest,
} from "@/lib/api";
import { isCustomerRole, canAccessAgentWorkspace } from "@/lib/role-policy";
import type { SettingsLoaderData } from "@/lib/loaders";
import { parseApiError } from "@/lib/api-error";
import {
  AccountSettingsScreen,
  type PasswordForm,
  type ProfileForm,
} from "@/widgets/account-settings/account-settings-screen";

function SettingsSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-10 w-full max-w-md" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

function toProfilePayload(form: ProfileForm, role: UserRole | undefined): ProfileUpdateRequest {
  if (!isCustomerRole(role)) {
    return {
      email: form.email,
      fullName: form.fullName,
    };
  }
  return {
    email: form.email,
    fullName: form.fullName,
    companyName: form.companyName,
    phoneNumber: form.phoneNumber,
    address: form.address,
    city: form.city,
    countryIso2: form.countryIso2,
    customerTierCode: form.customerTierCode,
    preferredLanguageCode: form.preferredLanguageCode,
  };
}

function applyProfileToForm(profile: ProfileResponse, reset: (values: ProfileForm) => void) {
  const customerProfile = profile.customerProfile;
  reset({
    email: profile.user.email || "",
    fullName: profile.user.fullName || "",
    companyName: customerProfile?.companyName || "",
    phoneNumber: customerProfile?.phoneNumber || "",
    address: customerProfile?.address || "",
    city: customerProfile?.city || "",
    countryIso2: customerProfile?.countryIso2 || "",
    customerTierCode: customerProfile?.customerTierCode || "",
    preferredLanguageCode: customerProfile?.preferredLanguageCode || "",
  });
}

export default function AccountSettingsPage() {
  const loaderData = useLoaderData<SettingsLoaderData>();
  const queryClient = useQueryClient();
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [settingsSuccess, setSettingsSuccess] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [profileGlobalError, setProfileGlobalError] = useState<string | null>(null);
  const [passwordGlobalError, setPasswordGlobalError] = useState<string | null>(null);
  const [settingsGlobalError, setSettingsGlobalError] = useState<string | null>(null);

  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
    initialData: loaderData.profile,
  });
  const { data: settings, isLoading: isSettingsLoading } = useQuery({
    queryKey: ["settings"],
    queryFn: getSettings,
    initialData: loaderData.settings,
  });
  const { data: signupOptions } = useQuery({
    queryKey: ["signup-options"],
    queryFn: getSignupOptions,
    initialData: loaderData.signupOptions ?? undefined,
  });

  const {
    control: profileControl,
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    reset: resetProfile,
    formState: { errors: profileErrors },
  } = useForm<ProfileForm>({
    defaultValues: {
      email: "",
      fullName: "",
      companyName: "",
      phoneNumber: "",
      address: "",
      city: "",
      countryIso2: "",
      customerTierCode: "",
      preferredLanguageCode: "",
    },
  });

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    reset: resetPassword,
    formState: { errors: passwordErrors },
  } = useForm<PasswordForm>({
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmNewPassword: "",
    },
  });

  const [localSettings, setLocalSettings] = useState<UserSettingsUpdateRequest>({});

  const updateProfileMutation = useUpdateProfileMutation();
  const updateSettingsMutation = useUpdateSettingsMutation();
  const changePasswordMutation = useChangePasswordMutation();

  useEffect(() => {
    if (!profile) return;
    applyProfileToForm(profile, resetProfile);
  }, [profile, resetProfile]);

  useEffect(() => {
    if (!settings) return;
    setLocalSettings({
      defaultLanding: settings.defaultLanding,
      sidebarCollapsed: settings.sidebarCollapsed,
      theme: settings.theme,
      compactMode: settings.compactMode,
      emailNotificationsEnabled: settings.emailNotificationsEnabled,
      notifyTicketReply: settings.notifyTicketReply,
      notifyStatusChange: settings.notifyStatusChange,
      notifyEscalation: settings.notifyEscalation,
    });
  }, [settings]);

  if (isProfileLoading || isSettingsLoading || !profile || !settings) {
    return <SettingsSkeleton />;
  }

  const user = profile.user;
  const customerRole = profile.profileContext === "CUSTOMER" && isCustomerRole(user.role);
  const isAgent = canAccessAgentWorkspace(user.role);

  const onProfileSubmit = async (form: ProfileForm) => {
    setProfileSuccess(false);
    setServerFieldErrors({});
    setProfileGlobalError(null);

    try {
      await updateProfileMutation.mutateAsync(toProfilePayload(form, user.role));
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      await queryClient.invalidateQueries({ queryKey: ["profile"] });
      setProfileSuccess(true);
    } catch (error) {
      const parsedError = parseApiError(error);
      setServerFieldErrors(parsedError.fieldErrors);
      setProfileGlobalError(parsedError.globalErrors[0] || parsedError.detail || "Profile update failed.");
    }
  };

  const onPasswordSubmit = async (form: PasswordForm) => {
    setPasswordSuccess(false);
    setPasswordGlobalError(null);

    try {
      await changePasswordMutation.mutateAsync({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
        confirmNewPassword: form.confirmNewPassword,
      });
      resetPassword();
      setPasswordSuccess(true);
    } catch (error) {
      const parsedError = parseApiError(error);
      setPasswordGlobalError(
        parsedError.fieldErrors.currentPassword ||
        parsedError.globalErrors[0] ||
        parsedError.detail ||
        "Password change failed."
      );
    }
  };

  const onSettingsSave = async () => {
    setSettingsSuccess(false);
    setSettingsGlobalError(null);

    try {
      await updateSettingsMutation.mutateAsync(localSettings);
      await queryClient.invalidateQueries({ queryKey: ["settings"] });
      setSettingsSuccess(true);
    } catch {
      setSettingsGlobalError("Unable to save settings. Please try again.");
    }
  };

  const getFieldError = (field: keyof ProfileForm, message?: string) => {
    if (message) return message;
    return serverFieldErrors[field];
  };

  const toggleSetting = (key: keyof UserSettingsUpdateRequest, value: boolean) => {
    setSettingsSuccess(false);
    setLocalSettings((prev) => ({ ...prev, [key]: value }));
  };

  const updateSetting = (key: keyof UserSettingsUpdateRequest, value: string) => {
    setSettingsSuccess(false);
    setLocalSettings((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <AccountSettingsScreen
      profile={profile}
      settings={settings}
      signupOptions={signupOptions}
      customerRole={customerRole}
      isAgent={isAgent}
      profileSuccess={profileSuccess}
      settingsSuccess={settingsSuccess}
      passwordSuccess={passwordSuccess}
      showCurrentPassword={showCurrentPassword}
      showNewPassword={showNewPassword}
      localSettings={localSettings}
      isProfileSubmitting={updateProfileMutation.isPending}
      isPasswordSubmitting={changePasswordMutation.isPending}
      profileControl={profileControl}
      registerProfile={registerProfile}
      registerPassword={registerPassword}
      handleProfileSubmit={handleProfileSubmit}
      handlePasswordSubmit={handlePasswordSubmit}
      profileErrors={profileErrors}
      passwordErrors={passwordErrors}
      onProfileSubmit={onProfileSubmit}
      onPasswordSubmit={onPasswordSubmit}
      onSettingsSave={onSettingsSave}
      setShowCurrentPassword={setShowCurrentPassword}
      setShowNewPassword={setShowNewPassword}
      toggleSetting={toggleSetting}
      updateSetting={updateSetting}
      getFieldError={getFieldError}
      profileGlobalError={profileGlobalError}
      passwordGlobalError={passwordGlobalError}
      settingsGlobalError={settingsGlobalError}
    />
  );
}
