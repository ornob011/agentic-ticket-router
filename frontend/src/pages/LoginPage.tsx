import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { getSettings } from "@/app/auth";
import { getTicketMetadata } from "@/app/tickets";
import type { UserRole } from "@/lib/api";
import { parseApiError } from "@/lib/api-error";
import { useLoginMutation } from "@/lib/hooks";
import { canAccessAgentWorkspace } from "@/lib/role-policy";
import { appRoutes } from "@/lib/routes";
import { LoginScreen, type LoginForm } from "@/widgets/login/login-screen";

const DEFAULT_LOGIN_ERROR = "Unable to sign in right now. Please try again.";

async function resolveLandingPath(defaultLanding: string, role: UserRole): Promise<string> {
  if (defaultLanding === "TICKETS") {
    return appRoutes.tickets.list;
  }

  if (defaultLanding === "QUEUE" && canAccessAgentWorkspace(role)) {
    if (role === "SUPERVISOR" || role === "ADMIN") {
      return appRoutes.agent.queues.all;
    }

    try {
      const metadata = await getTicketMetadata();
      const firstQueue = metadata.accessibleQueues[0]?.code;
      if (firstQueue) {
        return appRoutes.agent.queues.byCode(firstQueue);
      }
    } catch {
      return appRoutes.dashboard;
    }

    return appRoutes.dashboard;
  }

  return appRoutes.dashboard;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [formError, setFormError] = useState("");

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<LoginForm>({
    defaultValues: {
      username: "",
      password: "",
      rememberMe: true,
    },
  });

  const rememberMe = watch("rememberMe");
  const loginMutation = useLoginMutation();

  const onSubmit = useCallback(async (payload: LoginForm) => {
    setFormError("");

    const username = payload.username.trim();

    try {
      const authenticatedUser = await loginMutation.mutateAsync({
        username,
        password: payload.password,
        rememberMe: payload.rememberMe,
      });

      let landingPath: string;
      try {
        const settings = await getSettings();
        landingPath = await resolveLandingPath(settings.defaultLanding, authenticatedUser.role);
      } catch {
        landingPath = appRoutes.dashboard;
      }

      void navigate(landingPath, { replace: true });
    } catch (error) {
      const parsedError = parseApiError(error);
      const errorMessage = parsedError.fieldErrors.username
        || parsedError.fieldErrors.password
        || parsedError.globalErrors[0]
        || parsedError.detail
        || DEFAULT_LOGIN_ERROR;
      setFormError(errorMessage);
    }
  }, [loginMutation, navigate]);

  const handleRememberMeChange = useCallback((checked: boolean) => {
    setValue("rememberMe", checked, { shouldDirty: true });
  }, [setValue]);

  return (
    <LoginScreen
      formError={formError}
      errors={errors}
      rememberMe={rememberMe}
      isSubmitting={loginMutation.isPending}
      register={register}
      handleSubmit={handleSubmit}
      onSubmit={onSubmit}
      onRememberMeChange={handleRememberMeChange}
      onNavigateSignup={() => void navigate(appRoutes.signup)}
    />
  );
}
