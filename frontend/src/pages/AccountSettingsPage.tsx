import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { Controller, useForm } from "react-hook-form";
import { useLoaderData } from "react-router-dom";
import { getProfile, getSettings, getSignupOptions, updateProfile, updateSettings, changePassword } from "@/app/auth";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";
import {
  Building2,
  Globe,
  Save,
  UserCircle2,
  Settings2,
  Bell,
  Shield,
  Lock,
  LayoutGrid,
  Eye,
  EyeOff,
} from "lucide-react";
import type {
  ProfileResponse,
  ProfileUpdateRequest,
  SignupOptionsResponse,
  UserSettingsResponse,
  UserSettingsUpdateRequest,
  UserRole,
} from "@/lib/api";
import { getRoleBadgeVariant, isCustomerRole, canAccessAgentWorkspace } from "@/lib/role-policy";
import { formatRelativeTime } from "@/lib/utils";
import type { SettingsLoaderData } from "@/lib/loaders";

type ProfileForm = {
  email: string;
  fullName: string;
  companyName: string;
  phoneNumber: string;
  address: string;
  city: string;
  countryIso2: string;
  customerTierCode: string;
  preferredLanguageCode: string;
};

type PasswordForm = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

type ProblemDetailError = {
  detail?: string;
  errors?: string[];
  fieldErrors?: Record<string, string>;
};

type AccountSettingsViewProps = Readonly<{
  profile: ProfileResponse;
  settings: UserSettingsResponse;
  signupOptions: SignupOptionsResponse | undefined;
  customerRole: boolean;
  isAgent: boolean;
  profileError: string;
  profileSuccess: boolean;
  settingsSuccess: boolean;
  settingsError: string;
  passwordSuccess: boolean;
  passwordError: string;
  showCurrentPassword: boolean;
  showNewPassword: boolean;
  localSettings: UserSettingsUpdateRequest;
  isProfileSubmitting: boolean;
  isPasswordSubmitting: boolean;
  profileControl: ReturnType<typeof useForm<ProfileForm>>["control"];
  registerProfile: ReturnType<typeof useForm<ProfileForm>>["register"];
  registerPassword: ReturnType<typeof useForm<PasswordForm>>["register"];
  handleProfileSubmit: ReturnType<typeof useForm<ProfileForm>>["handleSubmit"];
  handlePasswordSubmit: ReturnType<typeof useForm<PasswordForm>>["handleSubmit"];
  profileErrors: ReturnType<typeof useForm<ProfileForm>>["formState"]["errors"];
  passwordErrors: ReturnType<typeof useForm<PasswordForm>>["formState"]["errors"];
  onProfileSubmit: (form: ProfileForm) => Promise<void>;
  onPasswordSubmit: (form: PasswordForm) => Promise<void>;
  onSettingsSave: () => Promise<void>;
  setShowCurrentPassword: React.Dispatch<React.SetStateAction<boolean>>;
  setShowNewPassword: React.Dispatch<React.SetStateAction<boolean>>;
  toggleSetting: (key: keyof UserSettingsUpdateRequest, value: boolean) => void;
  updateSetting: (key: keyof UserSettingsUpdateRequest, value: string) => void;
  fieldError: (field: keyof ProfileForm, message?: string) => string | undefined;
}>;

function toProblemDetailError(rawError: unknown): ProblemDetailError | null {
  if (!(rawError instanceof AxiosError)) {
    return null;
  }
  return rawError.response?.data ?? null;
}

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
  const loaderData = useLoaderData() as SettingsLoaderData;
  const queryClient = useQueryClient();
  const [profileError, setProfileError] = useState("");
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [settingsSuccess, setSettingsSuccess] = useState(false);
  const [settingsError, setSettingsError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

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
    formState: { errors: profileErrors, isSubmitting: isProfileSubmitting },
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
    formState: { errors: passwordErrors, isSubmitting: isPasswordSubmitting },
  } = useForm<PasswordForm>({
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmNewPassword: "",
    },
  });

  const [localSettings, setLocalSettings] = useState<UserSettingsUpdateRequest>({});

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
    setProfileError("");
    setProfileSuccess(false);
    setServerFieldErrors({});

    try {
      await updateProfile(toProfilePayload(form, user.role));
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      await queryClient.invalidateQueries({ queryKey: ["profile"] });
      setProfileSuccess(true);
    } catch (rawError) {
      const errorPayload = toProblemDetailError(rawError);
      const fieldErrors = errorPayload?.fieldErrors || {};
      setServerFieldErrors(fieldErrors);
      const firstError = errorPayload?.errors?.[0];
      const detail = errorPayload?.detail;
      setProfileError(firstError || detail || "Profile update failed.");
    }
  };

  const onPasswordSubmit = async (form: PasswordForm) => {
    setPasswordError("");
    setPasswordSuccess(false);

    try {
      await changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
        confirmNewPassword: form.confirmNewPassword,
      });
      resetPassword();
      setPasswordSuccess(true);
    } catch (rawError) {
      const errorPayload = toProblemDetailError(rawError);
      const firstError = errorPayload?.errors?.[0];
      const detail = errorPayload?.detail;
      const fieldErrors = errorPayload?.fieldErrors || {};
      const fieldError = Object.values(fieldErrors)[0];
      setPasswordError(fieldError || firstError || detail || "Password change failed.");
    }
  };

  const onSettingsSave = async () => {
    setSettingsSuccess(false);
    setSettingsError("");
    try {
      await updateSettings(localSettings);
      await queryClient.invalidateQueries({ queryKey: ["settings"] });
      setSettingsSuccess(true);
    } catch {
      setSettingsError("Unable to save settings. Please try again.");
    }
  };

  const fieldError = (field: keyof ProfileForm, message?: string) => {
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
    <AccountSettingsView
      profile={profile}
      settings={settings}
      signupOptions={signupOptions}
      customerRole={customerRole}
      isAgent={isAgent}
      profileError={profileError}
      profileSuccess={profileSuccess}
      settingsSuccess={settingsSuccess}
      settingsError={settingsError}
      passwordSuccess={passwordSuccess}
      passwordError={passwordError}
      showCurrentPassword={showCurrentPassword}
      showNewPassword={showNewPassword}
      localSettings={localSettings}
      isProfileSubmitting={isProfileSubmitting}
      isPasswordSubmitting={isPasswordSubmitting}
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
      fieldError={fieldError}
    />
  );
}

function AccountSettingsView(props: AccountSettingsViewProps) {
  return <AccountSettingsScreen {...props} />;
}

function AccountSettingsScreen(props: AccountSettingsViewProps) {
  return (
    <div className="space-y-6">
      <div className="gradient-header -mx-6 -mt-6 mb-6 px-6 py-6">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Account Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Manage your profile, preferences, notifications, and security settings.
        </p>
      </div>

      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList className="grid w-full grid-cols-4 lg:w-auto lg:inline-grid">
          <TabsTrigger value="profile" className="gap-2">
            <UserCircle2 className="h-4 w-4" />
            <span className="hidden sm:inline">Profile</span>
          </TabsTrigger>
          <TabsTrigger value="preferences" className="gap-2">
            <Settings2 className="h-4 w-4" />
            <span className="hidden sm:inline">Preferences</span>
          </TabsTrigger>
          <TabsTrigger value="notifications" className="gap-2">
            <Bell className="h-4 w-4" />
            <span className="hidden sm:inline">Notifications</span>
          </TabsTrigger>
          <TabsTrigger value="security" className="gap-2">
            <Shield className="h-4 w-4" />
            <span className="hidden sm:inline">Security</span>
          </TabsTrigger>
        </TabsList>
        <ProfileTabContent {...props} />
        <PreferencesTabContent {...props} />
        <NotificationsTabContent {...props} />
        <SecurityTabContent {...props} />
      </Tabs>
    </div>
  );
}

function ProfileTabContent(props: AccountSettingsViewProps) {
  const {
    profile,
    signupOptions,
    customerRole,
    profileError,
    profileSuccess,
    isProfileSubmitting,
    profileControl,
    registerProfile,
    handleProfileSubmit,
    profileErrors,
    onProfileSubmit,
    fieldError,
  } = props;

  const user = profile.user;

  return (
    <TabsContent value="profile" className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <UserCircle2 className="h-4 w-4 text-primary" />
            Account Identity
          </CardTitle>
          <CardDescription>Your basic account information</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={getRoleBadgeVariant(user.role)}>{user.roleLabel || user.role}</Badge>
            <Badge variant="outline">{user.username}</Badge>
            <Badge variant={profile.accountActive ? "success" : "destructive"}>
              {profile.accountActive ? "Active" : "Inactive"}
            </Badge>
            {profile.accountCreatedAt ? (
              <Badge variant="outline" className="text-xs">
                Joined {formatRelativeTime(profile.accountCreatedAt)}
              </Badge>
            ) : null}
          </div>
          <Separator />
          <form onSubmit={handleProfileSubmit(onProfileSubmit)} className="space-y-5">
            <BannerMessage
              message={profileError}
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="profile-email">Email</Label>
                <Input
                  id="profile-email"
                  autoComplete="email"
                  {...registerProfile("email", {
                    required: "Email is required",
                    maxLength: { value: 100, message: "Email must be at most 100 characters" },
                  })}
                />
                <FieldErrorText message={fieldError("email", profileErrors.email?.message)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="profile-full-name">Full Name</Label>
                <Input
                  id="profile-full-name"
                  autoComplete="name"
                  {...registerProfile("fullName", {
                    required: "Full name is required",
                    maxLength: { value: 100, message: "Full name must be at most 100 characters" },
                  })}
                />
                <FieldErrorText message={fieldError("fullName", profileErrors.fullName?.message)} />
              </div>
            </div>

            {customerRole ? (
              <CustomerProfileSection
                fieldError={fieldError}
                profileControl={profileControl}
                profileErrors={profileErrors}
                registerProfile={registerProfile}
                signupOptions={signupOptions}
              />
            ) : null}

            <div className="flex flex-wrap items-center gap-2">
              <Button type="submit" disabled={isProfileSubmitting}>
                <Save className="mr-2 h-4 w-4" />
                {isProfileSubmitting ? "Saving..." : "Save Profile"}
              </Button>
              {profileSuccess ? <Badge variant="success">Profile updated</Badge> : null}
            </div>
          </form>
        </CardContent>
      </Card>

      {!customerRole && profile.staffProfile ? <StaffContextCard profile={profile} /> : null}
    </TabsContent>
  );
}

function CustomerProfileSection(props: Readonly<{
  fieldError: AccountSettingsViewProps["fieldError"];
  profileControl: AccountSettingsViewProps["profileControl"];
  profileErrors: AccountSettingsViewProps["profileErrors"];
  registerProfile: AccountSettingsViewProps["registerProfile"];
  signupOptions: AccountSettingsViewProps["signupOptions"];
}>) {
  const { fieldError, profileControl, profileErrors, registerProfile, signupOptions } = props;

  return (
    <div className="space-y-5 rounded-lg border bg-muted/20 p-4">
      <div className="flex items-center gap-2">
        <Building2 className="h-4 w-4 text-primary" />
        <h3 className="text-sm font-semibold">Customer Profile</h3>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="profile-company">Company Name</Label>
          <Input
            id="profile-company"
            {...registerProfile("companyName", {
              required: "Company name is required",
              maxLength: { value: 100, message: "Company name must be at most 100 characters" },
            })}
          />
          <FieldErrorText message={fieldError("companyName", profileErrors.companyName?.message)} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="profile-phone">Phone Number</Label>
          <Input
            id="profile-phone"
            {...registerProfile("phoneNumber", {
              required: "Phone number is required",
              maxLength: { value: 20, message: "Phone number must be at most 20 characters" },
            })}
          />
          <FieldErrorText message={fieldError("phoneNumber", profileErrors.phoneNumber?.message)} />
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="profile-address">Address</Label>
          <Input
            id="profile-address"
            {...registerProfile("address", {
              required: "Address is required",
              maxLength: { value: 255, message: "Address must be at most 255 characters" },
            })}
          />
          <FieldErrorText message={fieldError("address", profileErrors.address?.message)} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="profile-city">City</Label>
          <Input
            id="profile-city"
            {...registerProfile("city", {
              required: "City is required",
              maxLength: { value: 100, message: "City must be at most 100 characters" },
            })}
          />
          <FieldErrorText message={fieldError("city", profileErrors.city?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Country</Label>
          <Controller
            name="countryIso2"
            control={profileControl}
            rules={{ required: "Country is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Select country" />
                </SelectTrigger>
                <SelectContent>
                  {(signupOptions?.countries || []).map((country) => (
                    <SelectItem key={country.code} value={country.code}>
                      {country.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldErrorText message={fieldError("countryIso2", profileErrors.countryIso2?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Tier</Label>
          <Controller
            name="customerTierCode"
            control={profileControl}
            rules={{ required: "Tier is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Select tier" />
                </SelectTrigger>
                <SelectContent>
                  {(signupOptions?.tiers || []).map((tier) => (
                    <SelectItem key={tier.code} value={tier.code}>
                      {tier.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldErrorText message={fieldError("customerTierCode", profileErrors.customerTierCode?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Preferred Language</Label>
          <Controller
            name="preferredLanguageCode"
            control={profileControl}
            rules={{ required: "Preferred language is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Select language" />
                </SelectTrigger>
                <SelectContent>
                  {(signupOptions?.languages || []).map((language) => (
                    <SelectItem key={language.code} value={language.code}>
                      {language.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldErrorText message={fieldError("preferredLanguageCode", profileErrors.preferredLanguageCode?.message)} />
        </div>
      </div>
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Globe className="h-3.5 w-3.5" />
        Customer profile fields are used for routing and personalization.
      </div>
    </div>
  );
}

function StaffContextCard(props: Readonly<{ profile: AccountSettingsViewProps["profile"] }>) {
  const { profile } = props;
  const staffProfile = profile.staffProfile;
  if (!staffProfile) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Building2 className="h-4 w-4 text-primary" />
          Staff Context
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Account Active</p>
            <Badge variant={staffProfile.active ? "success" : "destructive"}>{staffProfile.active ? "Yes" : "No"}</Badge>
          </div>
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Email Verified</p>
            <Badge variant={staffProfile.emailVerified ? "success" : "secondary"}>
              {staffProfile.emailVerified ? "Yes" : "No"}
            </Badge>
          </div>
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last Login</p>
            <p className="text-sm text-foreground">{staffProfile.lastLoginAt ? formatRelativeTime(staffProfile.lastLoginAt) : "-"}</p>
          </div>
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last Login IP</p>
            <p className="font-mono text-sm text-foreground">{staffProfile.lastLoginIp || "-"}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function PreferencesTabContent(props: AccountSettingsViewProps) {
  const { settings, isAgent, localSettings, settingsSuccess, settingsError, onSettingsSave, toggleSetting, updateSetting } = props;

  return (
    <TabsContent value="preferences" className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <LayoutGrid className="h-4 w-4 text-primary" />
            Workspace Behavior
          </CardTitle>
          <CardDescription>Customize how the application behaves when you use it</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="default-landing">Default Landing Page</Label>
              <Select
                value={localSettings.defaultLanding || settings.defaultLanding}
                onValueChange={(value) => updateSetting("defaultLanding", value)}
              >
                <SelectTrigger id="default-landing">
                  <SelectValue placeholder="Select default landing page" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DASHBOARD">Dashboard</SelectItem>
                  <SelectItem value="TICKETS">My Tickets</SelectItem>
                  {isAgent ? <SelectItem value="QUEUE">Queue Inbox</SelectItem> : null}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">Applied automatically after sign-in.</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="theme">Theme</Label>
              <Select value={localSettings.theme || settings.theme} onValueChange={(value) => updateSetting("theme", value)}>
                <SelectTrigger id="theme">
                  <SelectValue placeholder="Select theme" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LIGHT">Light</SelectItem>
                  <SelectItem value="DARK">Dark</SelectItem>
                  <SelectItem value="SYSTEM">System</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">Choose your preferred color scheme.</p>
            </div>
          </div>

          <Separator />

          <div className="space-y-4">
            <h4 className="text-sm font-medium">Display Options</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between rounded-lg border p-4">
                <div className="space-y-0.5">
                  <p className="text-sm font-medium">Collapsed Sidebar</p>
                  <p className="text-xs text-muted-foreground">Start with the sidebar minimized.</p>
                </div>
                <Switch
                  checked={localSettings.sidebarCollapsed ?? settings.sidebarCollapsed}
                  onCheckedChange={(checked) => toggleSetting("sidebarCollapsed", checked)}
                />
              </div>
              <div className="flex items-center justify-between rounded-lg border p-4">
                <div className="space-y-0.5">
                  <p className="text-sm font-medium">Compact Mode</p>
                  <p className="text-xs text-muted-foreground">Reduce spacing for denser information display.</p>
                </div>
                <Switch
                  checked={localSettings.compactMode ?? settings.compactMode}
                  onCheckedChange={(checked) => toggleSetting("compactMode", checked)}
                />
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button onClick={onSettingsSave}>
              <Save className="mr-2 h-4 w-4" />
              Save Preferences
            </Button>
            {settingsSuccess ? <Badge variant="success">Saved</Badge> : null}
            <FieldErrorText message={settingsError} />
          </div>
        </CardContent>
      </Card>
    </TabsContent>
  );
}

function NotificationsTabContent(props: AccountSettingsViewProps) {
  const { settings, isAgent, localSettings, settingsSuccess, settingsError, onSettingsSave, toggleSetting } = props;

  return (
    <TabsContent value="notifications" className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Bell className="h-4 w-4 text-primary" />
            Notification Preferences
          </CardTitle>
          <CardDescription>Control how and when you receive notifications</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-between rounded-lg border p-4">
            <div className="space-y-0.5">
              <p className="text-sm font-medium">Email Notifications</p>
              <p className="text-xs text-muted-foreground">Receive notifications via email.</p>
            </div>
            <Switch
              checked={localSettings.emailNotificationsEnabled ?? settings.emailNotificationsEnabled}
              onCheckedChange={(checked) => toggleSetting("emailNotificationsEnabled", checked)}
            />
          </div>

          <Separator />

          <div className="space-y-4">
            <h4 className="text-sm font-medium">Event Notifications</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between rounded-lg border p-4">
                <div className="space-y-0.5">
                  <p className="text-sm font-medium">Ticket Replies</p>
                  <p className="text-xs text-muted-foreground">When someone replies to your ticket.</p>
                </div>
                <Switch
                  checked={localSettings.notifyTicketReply ?? settings.notifyTicketReply}
                  onCheckedChange={(checked) => toggleSetting("notifyTicketReply", checked)}
                />
              </div>
              <div className="flex items-center justify-between rounded-lg border p-4">
                <div className="space-y-0.5">
                  <p className="text-sm font-medium">Status Changes</p>
                  <p className="text-xs text-muted-foreground">When a ticket status is updated.</p>
                </div>
                <Switch
                  checked={localSettings.notifyStatusChange ?? settings.notifyStatusChange}
                  onCheckedChange={(checked) => toggleSetting("notifyStatusChange", checked)}
                />
              </div>
              {isAgent ? (
                <div className="flex items-center justify-between rounded-lg border p-4">
                  <div className="space-y-0.5">
                    <p className="text-sm font-medium">Escalations</p>
                    <p className="text-xs text-muted-foreground">When a ticket is escalated.</p>
                  </div>
                  <Switch
                    checked={localSettings.notifyEscalation ?? settings.notifyEscalation}
                    onCheckedChange={(checked) => toggleSetting("notifyEscalation", checked)}
                  />
                </div>
              ) : null}
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button onClick={onSettingsSave}>
              <Save className="mr-2 h-4 w-4" />
              Save Notifications
            </Button>
            {settingsSuccess ? <Badge variant="success">Saved</Badge> : null}
            <FieldErrorText message={settingsError} />
          </div>
        </CardContent>
      </Card>
    </TabsContent>
  );
}

function SecurityTabContent(props: AccountSettingsViewProps) {
  const {
    profile,
    passwordSuccess,
    passwordError,
    showCurrentPassword,
    showNewPassword,
    isPasswordSubmitting,
    registerPassword,
    handlePasswordSubmit,
    passwordErrors,
    onPasswordSubmit,
    setShowCurrentPassword,
    setShowNewPassword,
  } = props;

  return (
    <TabsContent value="security" className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Lock className="h-4 w-4 text-primary" />
            Change Password
          </CardTitle>
          <CardDescription>Update your account password</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handlePasswordSubmit(onPasswordSubmit)} className="space-y-4">
            <BannerMessage
              message={passwordError}
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            />
            <BannerMessage
              message={passwordSuccess ? "Password changed successfully." : undefined}
              className="rounded-md border border-green-500/30 bg-green-500/10 px-3 py-2 text-sm text-green-600"
            />

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="current-password">Current Password</Label>
                <div className="relative">
                  <Input
                    id="current-password"
                    type={showCurrentPassword ? "text" : "password"}
                    {...registerPassword("currentPassword", { required: "Current password is required" })}
                  />
                  <button
                    type="button"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  >
                    {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <FieldErrorText message={passwordErrors.currentPassword?.message} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="new-password">New Password</Label>
                <div className="relative">
                  <Input
                    id="new-password"
                    type={showNewPassword ? "text" : "password"}
                    {...registerPassword("newPassword", {
                      required: "New password is required",
                      minLength: { value: 8, message: "Password must be at least 8 characters" },
                    })}
                  />
                  <button
                    type="button"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <FieldErrorText message={passwordErrors.newPassword?.message} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-new-password">Confirm New Password</Label>
                <Input
                  id="confirm-new-password"
                  type="password"
                  {...registerPassword("confirmNewPassword", {
                    required: "Please confirm your new password",
                    validate: (value, formValues) => value === formValues.newPassword || "Passwords do not match",
                  })}
                />
                <FieldErrorText message={passwordErrors.confirmNewPassword?.message} />
              </div>
            </div>

            <Button type="submit" disabled={isPasswordSubmitting}>
              <Lock className="mr-2 h-4 w-4" />
              {isPasswordSubmitting ? "Changing..." : "Change Password"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <AccountSecurityCard profile={profile} />
    </TabsContent>
  );
}

function AccountSecurityCard(props: Readonly<{ profile: AccountSettingsViewProps["profile"] }>) {
  const { profile } = props;
  const staffProfile = profile.staffProfile;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Shield className="h-4 w-4 text-primary" />
          Account Security
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Account Status</p>
            <Badge variant={profile.accountActive ? "success" : "destructive"}>
              {profile.accountActive ? "Active" : "Inactive"}
            </Badge>
          </div>
          {staffProfile ? (
            <>
              <div className="space-y-1">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Email Verification</p>
                <Badge variant={staffProfile.emailVerified ? "success" : "secondary"}>
                  {staffProfile.emailVerified ? "Verified" : "Not Verified"}
                </Badge>
              </div>
              {staffProfile.lastLoginAt ? (
                <div className="space-y-1">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last Login</p>
                  <p className="text-sm">{formatRelativeTime(staffProfile.lastLoginAt)}</p>
                </div>
              ) : null}
              {staffProfile.lastLoginIp ? (
                <div className="space-y-1">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last IP</p>
                  <p className="font-mono text-sm">{staffProfile.lastLoginIp}</p>
                </div>
              ) : null}
            </>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}

function BannerMessage(props: Readonly<{ className: string; message?: string }>) {
  const { className, message } = props;
  if (!message) {
    return null;
  }
  return <div className={className}>{message}</div>;
}

function FieldErrorText(props: Readonly<{ message?: string }>) {
  const { message } = props;
  if (!message) {
    return null;
  }
  return <p className="text-sm text-destructive">{message}</p>;
}
