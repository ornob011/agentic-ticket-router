import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { Controller, useForm } from "react-hook-form";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AlertCircle, ArrowLeft, Building2, Globe, Users, Shield } from "lucide-react";

type Option = { code: string; name: string };

type SignupForm = {
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

type SignupErrorResponse = {
  fieldErrors?: Array<{ field: keyof SignupForm; message: string }>;
  globalErrors?: string[];
  errors?: string[];
  detail?: string;
};

function toSignupErrorPayload(rawError: unknown): SignupErrorResponse | null {
  if (!(rawError instanceof AxiosError)) {
    return null;
  }

  return rawError.response?.data ?? null;
}

function toFieldErrorMap(
  payload: SignupErrorResponse | null,
): Partial<Record<keyof SignupForm, string>> {
  const fieldErrorMap: Partial<Record<keyof SignupForm, string>> = {};
  if (!payload?.fieldErrors?.length) {
    return fieldErrorMap;
  }

  for (const item of payload.fieldErrors) {
    fieldErrorMap[item.field] = item.message;
  }

  return fieldErrorMap;
}

function resolveSignupErrorMessage(payload: SignupErrorResponse | null): string {
  return payload?.errors?.[0]
    ?? payload?.globalErrors?.[0]
    ?? payload?.detail
    ?? "Signup failed.";
}

export default function SignupPage() {
  const navigate = useNavigate();
  const [countries, setCountries] = useState<Option[]>([]);
  const [tiers, setTiers] = useState<Option[]>([]);
  const [languages, setLanguages] = useState<Option[]>([]);
  const [formError, setFormError] = useState("");
  const [serverFieldErrors, setServerFieldErrors] = useState<Partial<Record<keyof SignupForm, string>>>({});

  const {
    control,
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<SignupForm>({
    defaultValues: {
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
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
  const passwordValue = watch("password");

  useEffect(() => {
    api.get<{ countries: Option[]; tiers: Option[]; languages: Option[] }>("/auth/signup-options").then((response) => {
      setCountries(response.data.countries);
      setTiers(response.data.tiers);
      setLanguages(response.data.languages);
    });
  }, []);

  const onSubmit = async (form: SignupForm) => {
    setFormError("");
    setServerFieldErrors({});
    try {
      await api.post("/auth/signup", form);
      navigate("/app/login");
    } catch (rawError) {
      const payload = toSignupErrorPayload(rawError);
      const normalizedFieldErrors = toFieldErrorMap(payload);
      const resolvedMessage = resolveSignupErrorMessage(payload);

      setServerFieldErrors(normalizedFieldErrors);
      setFormError(resolvedMessage);
    }
  };

  const fieldError = (field: keyof SignupForm, clientMessage?: string) => {
    if (clientMessage) {
      return clientMessage;
    }

    return serverFieldErrors[field];
  };

  const getSubmitLabel = () => {
    if (isSubmitting) {
      return "Creating account...";
    }

    return "Create Account";
  };

  return (
    <div className="min-h-dvh lg:h-screen flex bg-slate-50 overflow-hidden">
      <div className="hidden h-full lg:flex lg:w-[42%] xl:w-[46%] bg-gradient-to-b from-slate-300 via-blue-300 to-slate-200 p-10 xl:p-12 flex-col relative overflow-hidden border-r border-slate-300">
        <div className="absolute inset-0 opacity-[0.06]">
          <svg className="w-full h-full" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="hsl(215 16% 52%)" strokeWidth="1"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)" />
          </svg>
        </div>
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,hsl(217.2_91.2%_59.8%/_0.34),transparent_55%)]" />

        <div className="relative">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-slate-100/80 border border-slate-400/40 flex items-center justify-center">
              <Shield className="h-5 w-5 text-slate-800" />
            </div>
            <span className="text-2xl font-semibold text-slate-900">SupportHub</span>
          </div>
        </div>

        <div className="relative flex-1 flex flex-col justify-center space-y-8">
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-slate-700 font-medium">Customer Onboarding</p>
            <h1 className="mt-3 text-4xl xl:text-[2.6rem] font-semibold text-slate-900 leading-tight">
              Create Your Workspace
            </h1>
            <p className="mt-4 text-lg text-slate-700 max-w-xl">
              Create your organization account with standardized profile, region, and support tier configuration.
            </p>
          </div>

          <div className="grid sm:grid-cols-3 gap-4">
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Building2 className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">Organization Setup</p>
            </div>
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Globe className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">Regional Settings</p>
            </div>
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Users className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">Access Management</p>
            </div>
          </div>
        </div>

        <div className="relative text-sm text-slate-600">
          © 2026 SupportHub. All rights reserved.
        </div>
      </div>

      <div className="flex-1 min-h-dvh lg:h-full overflow-y-auto">
        <div className="min-h-dvh lg:min-h-full flex items-start justify-center p-4 sm:p-6 lg:p-8">
          <div className="w-full max-w-2xl py-4 lg:py-8">
            <div className="lg:hidden mb-5 rounded-xl border border-blue-200 bg-gradient-to-br from-blue-100 to-slate-100 p-4">
              <div className="flex items-center gap-2">
                <div className="h-9 w-9 rounded-lg bg-blue-600 flex items-center justify-center">
                  <Shield className="h-5 w-5 text-white" />
                </div>
                <span className="text-xl font-semibold text-slate-900">SupportHub</span>
              </div>
              <p className="mt-3 text-xs uppercase tracking-[0.2em] text-slate-700 font-medium">Customer Onboarding</p>
              <p className="mt-2 text-sm text-slate-700">Create your support workspace</p>
              <div className="mt-3 grid grid-cols-3 gap-2">
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Building2 className="mx-auto h-4 w-4 text-blue-700" />
                </div>
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Globe className="mx-auto h-4 w-4 text-blue-700" />
                </div>
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Users className="mx-auto h-4 w-4 text-blue-700" />
                </div>
              </div>
            </div>

            <Card className="border-0 shadow-xl">
              <CardHeader className="space-y-1">
                <CardTitle className="text-2xl">Create your account</CardTitle>
                <CardDescription>
                  Fill in your details to set up your customer workspace
                </CardDescription>
              </CardHeader>
              <CardContent>
                {formError && (
                  <div className="flex items-start gap-3 rounded-lg bg-destructive/10 p-4 mb-6">
                    <AlertCircle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
                    <div className="text-sm text-destructive">{formError}</div>
                  </div>
                )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="username">
                      Username <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="username"
                      placeholder="johndoe"
                      autoComplete="username"
                      {...register("username", { required: "Username is required" })}
                    />
                    {fieldError("username", errors.username?.message) && (
                      <p className="text-sm text-destructive">{fieldError("username", errors.username?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">
                      Email <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="john@company.com"
                      autoComplete="email"
                      {...register("email", { required: "Email is required" })}
                    />
                    {fieldError("email", errors.email?.message) && (
                      <p className="text-sm text-destructive">{fieldError("email", errors.email?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="password">
                      Password <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="password"
                      type="password"
                      placeholder="Create a password"
                      autoComplete="new-password"
                      {...register("password", { required: "Password is required" })}
                    />
                    {fieldError("password", errors.password?.message) && (
                      <p className="text-sm text-destructive">{fieldError("password", errors.password?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="confirmPassword">
                      Confirm Password <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      placeholder="Confirm password"
                      autoComplete="new-password"
                      {...register("confirmPassword", {
                        required: "Confirm password",
                        validate: (value) => value === passwordValue || "Passwords do not match",
                      })}
                    />
                    {fieldError("confirmPassword", errors.confirmPassword?.message) && (
                      <p className="text-sm text-destructive">{fieldError("confirmPassword", errors.confirmPassword?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="fullName">
                      Full Name <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="fullName"
                      placeholder="John Doe"
                      {...register("fullName", { required: "Full name is required" })}
                    />
                    {fieldError("fullName", errors.fullName?.message) && (
                      <p className="text-sm text-destructive">{fieldError("fullName", errors.fullName?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="companyName">
                      Company <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="companyName"
                      placeholder="Acme Inc."
                      {...register("companyName", { required: "Company is required" })}
                    />
                    {fieldError("companyName", errors.companyName?.message) && (
                      <p className="text-sm text-destructive">{fieldError("companyName", errors.companyName?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="phoneNumber">
                      Phone <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="phoneNumber"
                      placeholder="+1 555 000 1234"
                      {...register("phoneNumber", { required: "Phone is required" })}
                    />
                    {fieldError("phoneNumber", errors.phoneNumber?.message) && (
                      <p className="text-sm text-destructive">{fieldError("phoneNumber", errors.phoneNumber?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="city">
                      City <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="city"
                      placeholder="San Francisco"
                      {...register("city", { required: "City is required" })}
                    />
                    {fieldError("city", errors.city?.message) && (
                      <p className="text-sm text-destructive">{fieldError("city", errors.city?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor="address">
                      Address <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="address"
                      placeholder="123 Main Street"
                      {...register("address", { required: "Address is required" })}
                    />
                    {fieldError("address", errors.address?.message) && (
                      <p className="text-sm text-destructive">{fieldError("address", errors.address?.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label>
                      Country <span className="text-destructive">*</span>
                    </Label>
                    <Controller
                      name="countryIso2"
                      control={control}
                      rules={{ required: "Country is required" }}
                      render={({ field }) => (
                        <Select onValueChange={field.onChange} value={field.value || undefined}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select country" />
                          </SelectTrigger>
                          <SelectContent>
                            {countries.map((country) => (
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
                    <Label>
                      Customer Tier <span className="text-destructive">*</span>
                    </Label>
                    <Controller
                      name="customerTierCode"
                      control={control}
                      rules={{ required: "Customer tier is required" }}
                      render={({ field }) => (
                        <Select onValueChange={field.onChange} value={field.value || undefined}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select tier" />
                          </SelectTrigger>
                          <SelectContent>
                            {tiers.map((tier) => (
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

                  <div className="space-y-2 md:col-span-2">
                    <Label>
                      Preferred Language <span className="text-destructive">*</span>
                    </Label>
                    <Controller
                      name="preferredLanguageCode"
                      control={control}
                      rules={{ required: "Preferred language is required" }}
                      render={({ field }) => (
                        <Select onValueChange={field.onChange} value={field.value || undefined}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select language" />
                          </SelectTrigger>
                          <SelectContent>
                            {languages.map((language) => (
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

                <div className="flex flex-col-reverse sm:flex-row gap-3 pt-2">
                  <Button type="button" variant="outline" onClick={() => navigate("/app/login")}>
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Back to Sign In
                  </Button>
                  <Button type="submit" disabled={isSubmitting} className="flex-1 sm:flex-none">
                    {getSubmitLabel()}
                  </Button>
                </div>
              </form>
              </CardContent>
            </Card>

          <p className="mt-6 text-center text-xs text-muted-foreground">
            By creating an account, you agree to our Terms of Service and Privacy Policy.
          </p>
          </div>
        </div>
      </div>
    </div>
  );
}
