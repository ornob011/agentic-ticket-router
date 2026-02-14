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
    <div className="min-h-screen flex bg-slate-50">
      <div className="hidden lg:flex lg:w-1/2 xl:w-[55%] bg-gradient-to-br from-sky-500 via-blue-500 to-indigo-600 p-12 flex-col justify-between relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <svg className="w-full h-full" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="white" strokeWidth="1"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)" />
          </svg>
        </div>

        <div className="relative">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-white/20 flex items-center justify-center">
              <Shield className="h-6 w-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-white">SupportHub</span>
          </div>
        </div>

        <div className="relative space-y-8">
          <div>
            <h1 className="text-4xl xl:text-5xl font-bold text-white leading-tight">
              Start Your Journey<br />With Us Today
            </h1>
            <p className="mt-4 text-lg text-white/80 max-w-lg">
              Create your customer account and get access to enterprise-grade support with intelligent routing and real-time collaboration.
            </p>
          </div>

          <div className="grid sm:grid-cols-3 gap-4">
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Building2 className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">Organization Setup</p>
            </div>
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Globe className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">Localization</p>
            </div>
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Users className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">Team Access</p>
            </div>
          </div>
        </div>

        <div className="relative text-sm text-white/60">
          © 2026 SupportHub. All rights reserved.
        </div>
      </div>

      <div className="flex-1 flex items-start justify-center p-8 overflow-y-auto">
        <div className="w-full max-w-2xl py-8">
          <div className="lg:hidden flex items-center justify-center mb-8">
            <div className="flex items-center gap-2">
              <div className="h-10 w-10 rounded-xl bg-blue-500 flex items-center justify-center">
                <Shield className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold">SupportHub</span>
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
                    <Label htmlFor="username">Username *</Label>
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
                    <Label htmlFor="email">Email *</Label>
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
                    <Label htmlFor="password">Password *</Label>
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
                    <Label htmlFor="confirmPassword">Confirm Password *</Label>
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
                    <Label htmlFor="fullName">Full Name *</Label>
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
                    <Label htmlFor="companyName">Company *</Label>
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
                    <Label htmlFor="phoneNumber">Phone *</Label>
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
                    <Label htmlFor="city">City *</Label>
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
                    <Label htmlFor="address">Address *</Label>
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
                    <Label>Country *</Label>
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
                    <Label>Customer Tier *</Label>
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
                    <Label>Preferred Language *</Label>
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
  );
}
