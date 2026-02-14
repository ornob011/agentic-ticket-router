import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { Controller, useForm } from "react-hook-form";
import { getProfile, getSignupOptions, updateProfile } from "@/app/auth";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Building2, Globe, Save, UserCircle2 } from "lucide-react";
import type { ProfileResponse, UserRole } from "@/lib/api";

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

type ProblemDetailError = {
  detail?: string;
  errors?: string[];
  fieldErrors?: Record<string, string>;
};

function profileDefaultValues(): ProfileForm {
  return {
    email: "",
    fullName: "",
    companyName: "",
    phoneNumber: "",
    address: "",
    city: "",
    countryIso2: "",
    customerTierCode: "",
    preferredLanguageCode: "",
  };
}

function toProblemDetailError(rawError: unknown): ProblemDetailError | null {
  if (!(rawError instanceof AxiosError)) {
    return null;
  }

  return rawError.response?.data ?? null;
}

function ProfileSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full" />
      <Skeleton className="h-56 w-full" />
    </div>
  );
}

function isCustomer(role: UserRole | undefined): boolean {
  return role === "CUSTOMER";
}

function toProfilePayload(form: ProfileForm, role: UserRole | undefined) {
  if (!isCustomer(role)) {
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

function applyProfileToForm(
  profile: ProfileResponse,
  reset: (values: ProfileForm) => void
) {
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

export default function ProfilePage() {
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState("");
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [serverFieldErrors, setServerFieldErrors] = useState<Record<string, string>>({});

  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
  });
  const { data: signupOptions } = useQuery({
    queryKey: ["signup-options"],
    queryFn: getSignupOptions,
  });

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProfileForm>({
    defaultValues: profileDefaultValues(),
  });

  useEffect(() => {
    if (!profile) {
      return;
    }

    applyProfileToForm(profile, reset);
  }, [profile, reset]);

  if (isProfileLoading || !profile) {
    return <ProfileSkeleton />;
  }

  const user = profile.user;
  const customerRole = profile.profileContext === "CUSTOMER" && isCustomer(user.role);
  const onSubmit = async (form: ProfileForm) => {
    setFormError("");
    setSaveSuccess(false);
    setServerFieldErrors({});

    try {
      await updateProfile(toProfilePayload(form, user.role));
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      await queryClient.invalidateQueries({ queryKey: ["profile"] });
      setSaveSuccess(true);
    } catch (rawError) {
      const errorPayload = toProblemDetailError(rawError);
      const fieldErrors = errorPayload?.fieldErrors || {};
      setServerFieldErrors(fieldErrors);

      const firstError = errorPayload?.errors?.[0];
      const detail = errorPayload?.detail;
      setFormError(firstError || detail || "Profile update failed.");
    }
  };

  const fieldError = (field: keyof ProfileForm, message?: string) => {
    if (message) {
      return message;
    }

    return serverFieldErrors[field];
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Profile"
        description="Manage your account profile information."
      />

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <UserCircle2 className="h-4 w-4 text-primary" />
            Account Identity
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="secondary">{user.roleLabel || user.role}</Badge>
            <Badge variant="outline">{user.username}</Badge>
          </div>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {formError && (
              <div className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {formError}
              </div>
            )}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="profile-email">Email</Label>
                <Input
                  id="profile-email"
                  autoComplete="email"
                  {...register("email", {
                    required: "Email is required",
                    maxLength: {
                      value: 100,
                      message: "Email must be at most 100 characters",
                    },
                  })}
                />
                {fieldError("email", errors.email?.message) && (
                  <p className="text-sm text-destructive">{fieldError("email", errors.email?.message)}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="profile-full-name">Full Name</Label>
                <Input
                  id="profile-full-name"
                  autoComplete="name"
                  {...register("fullName", {
                    required: "Full name is required",
                    maxLength: {
                      value: 100,
                      message: "Full name must be at most 100 characters",
                    },
                  })}
                />
                {fieldError("fullName", errors.fullName?.message) && (
                  <p className="text-sm text-destructive">{fieldError("fullName", errors.fullName?.message)}</p>
                )}
              </div>
            </div>

            {customerRole && (
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
                      {...register("companyName", {
                        required: "Company name is required",
                        maxLength: {
                          value: 100,
                          message: "Company name must be at most 100 characters",
                        },
                      })}
                    />
                    {fieldError("companyName", errors.companyName?.message) && (
                      <p className="text-sm text-destructive">{fieldError("companyName", errors.companyName?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="profile-phone">Phone Number</Label>
                    <Input
                      id="profile-phone"
                      {...register("phoneNumber", {
                        required: "Phone number is required",
                        maxLength: {
                          value: 20,
                          message: "Phone number must be at most 20 characters",
                        },
                      })}
                    />
                    {fieldError("phoneNumber", errors.phoneNumber?.message) && (
                      <p className="text-sm text-destructive">{fieldError("phoneNumber", errors.phoneNumber?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="profile-address">Address</Label>
                    <Input
                      id="profile-address"
                      {...register("address", {
                        required: "Address is required",
                        maxLength: {
                          value: 255,
                          message: "Address must be at most 255 characters",
                        },
                      })}
                    />
                    {fieldError("address", errors.address?.message) && (
                      <p className="text-sm text-destructive">{fieldError("address", errors.address?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="profile-city">City</Label>
                    <Input
                      id="profile-city"
                      {...register("city", {
                        required: "City is required",
                        maxLength: {
                          value: 100,
                          message: "City must be at most 100 characters",
                        },
                      })}
                    />
                    {fieldError("city", errors.city?.message) && (
                      <p className="text-sm text-destructive">{fieldError("city", errors.city?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label>Country</Label>
                    <Controller
                      name="countryIso2"
                      control={control}
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
                    {fieldError("countryIso2", errors.countryIso2?.message) && (
                      <p className="text-sm text-destructive">{fieldError("countryIso2", errors.countryIso2?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label>Tier</Label>
                    <Controller
                      name="customerTierCode"
                      control={control}
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
                    {fieldError("customerTierCode", errors.customerTierCode?.message) && (
                      <p className="text-sm text-destructive">{fieldError("customerTierCode", errors.customerTierCode?.message)}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label>Preferred Language</Label>
                    <Controller
                      name="preferredLanguageCode"
                      control={control}
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
                    {fieldError("preferredLanguageCode", errors.preferredLanguageCode?.message) && (
                      <p className="text-sm text-destructive">{fieldError("preferredLanguageCode", errors.preferredLanguageCode?.message)}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Globe className="h-3.5 w-3.5" />
                  Customer profile fields are used for routing and personalization.
                </div>
              </div>
            )}

            <div className="flex flex-wrap items-center gap-2">
              <Button type="submit" disabled={isSubmitting}>
                <Save className="mr-2 h-4 w-4" />
                {isSubmitting ? "Saving..." : "Save Profile"}
              </Button>
              {saveSuccess && (
                <Badge variant="success">Profile updated</Badge>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      {!customerRole && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Building2 className="h-4 w-4 text-primary" />
              Staff Context
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Account Active</p>
              <p className="text-sm text-foreground">{profile.staffProfile?.active ? "Yes" : "No"}</p>
            </div>
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Email Verified</p>
              <p className="text-sm text-foreground">{profile.staffProfile?.emailVerified ? "Yes" : "No"}</p>
            </div>
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last Login</p>
              <p className="text-sm text-foreground">{profile.staffProfile?.lastLoginAt || "-"}</p>
            </div>
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Last Login IP</p>
              <p className="text-sm text-foreground">{profile.staffProfile?.lastLoginIp || "-"}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
