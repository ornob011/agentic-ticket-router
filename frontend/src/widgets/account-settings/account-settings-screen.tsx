import { Controller, type Control, type FieldErrors, type UseFormHandleSubmit, type UseFormRegister } from "react-hook-form";
import type { Dispatch, SetStateAction } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
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
  SignupOptionsResponse,
  UserSettingsResponse,
  UserSettingsUpdateRequest,
} from "@/lib/api";
import { getRoleBadgeVariant } from "@/lib/role-policy";
import { formatRelativeTime } from "@/lib/utils";

export type ProfileForm = {
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

export type PasswordForm = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

export type AccountSettingsViewProps = Readonly<{
  profile: ProfileResponse;
  settings: UserSettingsResponse;
  signupOptions: SignupOptionsResponse | undefined;
  customerRole: boolean;
  isAgent: boolean;
  profileSuccess: boolean;
  settingsSuccess: boolean;
  passwordSuccess: boolean;
  showCurrentPassword: boolean;
  showNewPassword: boolean;
  localSettings: UserSettingsUpdateRequest;
  isProfileSubmitting: boolean;
  isPasswordSubmitting: boolean;
  profileControl: Control<ProfileForm>;
  registerProfile: UseFormRegister<ProfileForm>;
  registerPassword: UseFormRegister<PasswordForm>;
  handleProfileSubmit: UseFormHandleSubmit<ProfileForm>;
  handlePasswordSubmit: UseFormHandleSubmit<PasswordForm>;
  profileErrors: FieldErrors<ProfileForm>;
  passwordErrors: FieldErrors<PasswordForm>;
  onProfileSubmit: (form: ProfileForm) => Promise<void>;
  onPasswordSubmit: (form: PasswordForm) => Promise<void>;
  onSettingsSave: () => Promise<void>;
  setShowCurrentPassword: Dispatch<SetStateAction<boolean>>;
  setShowNewPassword: Dispatch<SetStateAction<boolean>>;
  toggleSetting: (key: keyof UserSettingsUpdateRequest, value: boolean) => void;
  updateSetting: (key: keyof UserSettingsUpdateRequest, value: string) => void;
  getFieldError: (field: keyof ProfileForm, message?: string) => string | undefined;
  profileGlobalError: string | null;
  passwordGlobalError: string | null;
  settingsGlobalError: string | null;
}>;

export function AccountSettingsScreen(props: AccountSettingsViewProps) {
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
    profileSuccess,
    isProfileSubmitting,
    profileControl,
    registerProfile,
    handleProfileSubmit,
    profileErrors,
    onProfileSubmit,
    getFieldError,
    profileGlobalError,
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
              message={profileGlobalError}
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="profile-email">Email</Label>
                <Input
                  id="profile-email"
                  autoComplete="email"
                  disabled={isProfileSubmitting}
                  {...registerProfile("email", {
                    required: "Email is required",
                    maxLength: { value: 100, message: "Email must be at most 100 characters" },
                  })}
                />
                <FieldErrorText message={getFieldError("email", profileErrors.email?.message)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="profile-full-name">Full Name</Label>
                <Input
                  id="profile-full-name"
                  autoComplete="name"
                  disabled={isProfileSubmitting}
                  {...registerProfile("fullName", {
                    required: "Full name is required",
                    maxLength: { value: 100, message: "Full name must be at most 100 characters" },
                  })}
                />
                <FieldErrorText message={getFieldError("fullName", profileErrors.fullName?.message)} />
              </div>
            </div>

            {customerRole ? (
              <CustomerProfileSection
                getFieldError={getFieldError}
                profileControl={profileControl}
                profileErrors={profileErrors}
                registerProfile={registerProfile}
                signupOptions={signupOptions}
                disabled={isProfileSubmitting}
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
  getFieldError: AccountSettingsViewProps["getFieldError"];
  profileControl: AccountSettingsViewProps["profileControl"];
  profileErrors: AccountSettingsViewProps["profileErrors"];
  registerProfile: AccountSettingsViewProps["registerProfile"];
  signupOptions: AccountSettingsViewProps["signupOptions"];
  disabled: boolean;
}>) {
  const { getFieldError, profileControl, profileErrors, registerProfile, signupOptions, disabled } = props;

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
            disabled={disabled}
            {...registerProfile("companyName", {
              required: "Company name is required",
              maxLength: { value: 100, message: "Company name must be at most 100 characters" },
            })}
          />
          <FieldErrorText message={getFieldError("companyName", profileErrors.companyName?.message)} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="profile-phone">Phone Number</Label>
          <Input
            id="profile-phone"
            disabled={disabled}
            {...registerProfile("phoneNumber", {
              required: "Phone number is required",
              maxLength: { value: 20, message: "Phone number must be at most 20 characters" },
            })}
          />
          <FieldErrorText message={getFieldError("phoneNumber", profileErrors.phoneNumber?.message)} />
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="profile-address">Address</Label>
          <Input
            id="profile-address"
            disabled={disabled}
            {...registerProfile("address", {
              required: "Address is required",
              maxLength: { value: 255, message: "Address must be at most 255 characters" },
            })}
          />
          <FieldErrorText message={getFieldError("address", profileErrors.address?.message)} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="profile-city">City</Label>
          <Input
            id="profile-city"
            disabled={disabled}
            {...registerProfile("city", {
              required: "City is required",
              maxLength: { value: 100, message: "City must be at most 100 characters" },
            })}
          />
          <FieldErrorText message={getFieldError("city", profileErrors.city?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Country</Label>
          <Controller
            name="countryIso2"
            control={profileControl}
            rules={{ required: "Country is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange} disabled={disabled}>
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
          <FieldErrorText message={getFieldError("countryIso2", profileErrors.countryIso2?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Tier</Label>
          <Controller
            name="customerTierCode"
            control={profileControl}
            rules={{ required: "Tier is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange} disabled={disabled}>
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
          <FieldErrorText message={getFieldError("customerTierCode", profileErrors.customerTierCode?.message)} />
        </div>
        <div className="space-y-2">
          <Label>Preferred Language</Label>
          <Controller
            name="preferredLanguageCode"
            control={profileControl}
            rules={{ required: "Preferred language is required" }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange} disabled={disabled}>
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
          <FieldErrorText message={getFieldError("preferredLanguageCode", profileErrors.preferredLanguageCode?.message)} />
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
  const { settings, isAgent, localSettings, settingsSuccess, onSettingsSave, toggleSetting, updateSetting, settingsGlobalError } = props;

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
            <Button onClick={() => void onSettingsSave()}>
              <Save className="mr-2 h-4 w-4" />
              Save Preferences
            </Button>
            {settingsSuccess ? <Badge variant="success">Saved</Badge> : null}
            <FieldErrorText message={settingsGlobalError ?? undefined} />
          </div>
        </CardContent>
      </Card>
    </TabsContent>
  );
}

function NotificationsTabContent(props: AccountSettingsViewProps) {
  const { settings, isAgent, localSettings, settingsSuccess, onSettingsSave, toggleSetting, settingsGlobalError } = props;

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
            <Button onClick={() => void onSettingsSave()}>
              <Save className="mr-2 h-4 w-4" />
              Save Notifications
            </Button>
            {settingsSuccess ? <Badge variant="success">Saved</Badge> : null}
            <FieldErrorText message={settingsGlobalError ?? undefined} />
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
    showCurrentPassword,
    showNewPassword,
    isPasswordSubmitting,
    registerPassword,
    handlePasswordSubmit,
    passwordErrors,
    onPasswordSubmit,
    setShowCurrentPassword,
    setShowNewPassword,
    passwordGlobalError,
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
              message={passwordGlobalError}
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
                    disabled={isPasswordSubmitting}
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
                    disabled={isPasswordSubmitting}
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
                  disabled={isPasswordSubmitting}
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

function BannerMessage(props: Readonly<{ className: string; message?: string | null }>) {
  const { className, message } = props;
  if (!message) {
    return null;
  }
  return <div className={className}>{message}</div>;
}

function FieldErrorText(props: Readonly<{ message?: string | null }>) {
  const { message } = props;
  if (!message) {
    return null;
  }
  return <p className="text-sm text-destructive">{message}</p>;
}
