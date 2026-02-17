import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { getSignupOptions } from "@/app/auth";
import { parseApiError } from "@/lib/api-error";
import { useSignupMutation } from "@/lib/hooks";
import { appRoutes } from "@/lib/routes";
import { SignupScreen, type Option, type SignupForm } from "@/widgets/signup/signup-screen";

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
    formState: { errors },
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
  const signupMutation = useSignupMutation();

  useEffect(() => {
    getSignupOptions()
      .then((response) => {
        setCountries(response.countries);
        setTiers(response.tiers);
        setLanguages(response.languages);
      })
      .catch((rawError: unknown) => {
        const parsedError = parseApiError(rawError);
        const message = parsedError.detail.trim().length > 0
          ? parsedError.detail
          : "Failed to load signup options.";
        setFormError(message);
      });
  }, []);

  const handleSignupSubmit = async (form: SignupForm) => {
    setFormError("");
    setServerFieldErrors({});

    try {
      await signupMutation.mutateAsync(form);
      void navigate(appRoutes.login);
    } catch (error) {
      const parsedError = parseApiError(error);

      if (Object.keys(parsedError.fieldErrors).length > 0) {
        setServerFieldErrors(parsedError.fieldErrors as Partial<Record<keyof SignupForm, string>>);
      }

      const firstFieldError = Object.values(parsedError.fieldErrors)[0];
      const globalError = parsedError.globalErrors[0] || parsedError.detail;
      setFormError(firstFieldError || globalError || "Signup failed.");
    }
  };

  const getFieldError = (field: keyof SignupForm, clientMessage?: string) => {
    if (clientMessage) {
      return clientMessage;
    }

    const serverMessage = serverFieldErrors[field];
    return typeof serverMessage === "string" ? serverMessage : undefined;
  };

  return (
    <SignupScreen
      countries={countries}
      tiers={tiers}
      languages={languages}
      formError={formError}
      isSubmitting={signupMutation.isPending}
      passwordValue={passwordValue}
      control={control}
      register={register}
      handleSubmit={handleSubmit}
      errors={errors}
      onSubmit={handleSignupSubmit}
      onBackToSignIn={() => void navigate(appRoutes.login)}
      getFieldError={getFieldError}
    />
  );
}
