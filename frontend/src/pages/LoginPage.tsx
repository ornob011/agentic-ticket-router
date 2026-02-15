import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { getSettings, login } from "@/app/auth";
import { api, type TicketMetadataResponse } from "@/lib/api";
import { parseApiError } from "@/lib/api-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertCircle, ArrowRight, Clock, Shield, Zap } from "lucide-react";
import { canAccessAgentWorkspace } from "@/lib/role-policy";
import type { UserRole } from "@/lib/api";

type LoginForm = {
  username: string;
  password: string;
  rememberMe: boolean;
};

const DEFAULT_LOGIN_ERROR = "Unable to sign in right now. Please try again.";
const INVALID_CREDENTIALS_MESSAGE = "Invalid username or password. Please try again.";

async function resolveLandingPath(
  defaultLanding: string,
  role: UserRole
): Promise<string> {
  if (defaultLanding === "TICKETS") {
    return "/app/tickets";
  }

  if (defaultLanding === "QUEUE" && canAccessAgentWorkspace(role)) {
    if (role === "SUPERVISOR" || role === "ADMIN") {
      return "/app/agent/queues/ALL";
    }

    try {
      const response = await api.get<TicketMetadataResponse>("/tickets/meta");
      const firstQueue = response.data.accessibleQueues[0]?.code;
      if (firstQueue) {
        return `/app/agent/queues/${firstQueue}`;
      }
    } catch {
      return "/app/dashboard";
    }

    return "/app/dashboard";
  }

  return "/app/dashboard";
}

function resolveLoginErrorMessage(error: unknown): string {
  const parsedError = parseApiError(error);

  if (parsedError.status === 401 || parsedError.status === 403) {
    return INVALID_CREDENTIALS_MESSAGE;
  }

  return parsedError.detail || DEFAULT_LOGIN_ERROR;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [formError, setFormError] = useState("");
  const {
    register,
    handleSubmit,
    setValue,
    setError,
    clearErrors,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    defaultValues: {
      username: "",
      password: "",
      rememberMe: true,
    },
  });

  const rememberMe = watch("rememberMe");

  const onSubmit = useCallback(async (payload: LoginForm) => {
    setFormError("");
    clearErrors();

    const username = payload.username.trim();

    try {
      const authenticatedUser = await login(
        username,
        payload.password,
        payload.rememberMe
      );

      let landingPath: string;
      try {
        const settings = await getSettings();
        landingPath = await resolveLandingPath(
          settings.defaultLanding,
          authenticatedUser.role
        );
      } catch {
        landingPath = "/app/dashboard";
      }

      navigate(landingPath, { replace: true });
    } catch (error) {
      const parsedError = parseApiError(error);
      const usernameError = parsedError.fieldErrors.username;
      const passwordError = parsedError.fieldErrors.password;

      if (usernameError) {
        setError("username", {
          type: "server",
          message: usernameError,
        });
      }

      if (passwordError) {
        setError("password", {
          type: "server",
          message: passwordError,
        });
      }

      setFormError(resolveLoginErrorMessage(error));
    }
  }, [clearErrors, navigate, setError]);

  const handleRememberMeChange = useCallback((checked: boolean) => {
    setValue("rememberMe", checked, { shouldDirty: true });
  }, [setValue]);

  const renderSubmitContent = () => {
    if (isSubmitting) {
      return (
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
          Signing in...
        </div>
      );
    }

    return (
      <>
        Sign in
        <ArrowRight className="ml-2 h-4 w-4" />
      </>
    );
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
            <p className="text-xs uppercase tracking-[0.24em] text-slate-700 font-medium">Customer Support Platform</p>
            <h1 className="mt-3 text-4xl xl:text-[2.6rem] font-semibold text-slate-900 leading-tight">
              Enterprise Support Operations
            </h1>
            <p className="mt-4 text-lg text-slate-700 max-w-xl">
              Manage ticket intake, triage, and agent workflows with a secure, structured support workspace.
            </p>
          </div>

          <div className="grid sm:grid-cols-3 gap-4">
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Zap className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">Automated Routing</p>
            </div>
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Shield className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">SLA Governance</p>
            </div>
            <div className="rounded-lg bg-slate-100/35 p-4">
              <Clock className="h-5 w-5 text-blue-700 mb-2" />
              <p className="text-sm font-medium text-slate-800">Continuous Coverage</p>
            </div>
          </div>
        </div>

        <div className="relative text-sm text-slate-600">
          © 2026 SupportHub. All rights reserved.
        </div>
      </div>

      <div className="flex-1 min-h-dvh lg:h-full overflow-y-auto">
        <div className="min-h-dvh lg:min-h-full flex items-start lg:items-center justify-center p-4 sm:p-6 lg:p-8">
          <div className="w-full max-w-md">
            <div className="lg:hidden mb-5 rounded-xl border border-blue-200 bg-gradient-to-br from-blue-100 to-slate-100 p-4">
              <div className="flex items-center gap-2">
                <div className="h-9 w-9 rounded-lg bg-blue-600 flex items-center justify-center">
                  <Shield className="h-5 w-5 text-white" />
                </div>
                <span className="text-xl font-semibold text-slate-900">SupportHub</span>
              </div>
              <p className="mt-3 text-xs uppercase tracking-[0.2em] text-slate-700 font-medium">Customer Support Platform</p>
              <p className="mt-2 text-sm text-slate-700">Enterprise support operations</p>
              <div className="mt-3 grid grid-cols-3 gap-2">
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Zap className="mx-auto h-4 w-4 text-blue-700" />
                </div>
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Shield className="mx-auto h-4 w-4 text-blue-700" />
                </div>
                <div className="rounded-md bg-white/60 p-2 text-center">
                  <Clock className="mx-auto h-4 w-4 text-blue-700" />
                </div>
              </div>
            </div>

            <Card className="border-0 shadow-xl">
              <CardHeader className="space-y-1 pb-4">
                <CardTitle className="text-2xl">Sign in</CardTitle>
                <CardDescription>
                  Enter your credentials to access your workspace
                </CardDescription>
              </CardHeader>
              <CardContent>
                {formError && (
                  <div className="flex items-start gap-3 rounded-lg bg-destructive/10 p-4 mb-6" role="alert" aria-live="polite">
                    <AlertCircle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
                    <div className="text-sm text-destructive">{formError}</div>
                  </div>
                )}

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
                  <div className="space-y-2">
                    <Label htmlFor="username">Username</Label>
                    <Input
                      id="username"
                      placeholder="Enter your username"
                      autoComplete="username"
                      autoFocus
                      disabled={isSubmitting}
                      {...register("username", {
                        required: "Username is required",
                        setValueAs: (value: string) => value.trim(),
                      })}
                    />
                    {errors.username && (
                      <p className="text-sm text-destructive">{errors.username.message}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="password">Password</Label>
                    <Input
                      id="password"
                      type="password"
                      placeholder="Enter your password"
                      autoComplete="current-password"
                      disabled={isSubmitting}
                      {...register("password", { required: "Password is required" })}
                    />
                    {errors.password && (
                      <p className="text-sm text-destructive">{errors.password.message}</p>
                    )}
                  </div>

                  <div className="flex items-center gap-2">
                    <Switch
                      id="rememberMe"
                      checked={Boolean(rememberMe)}
                      onCheckedChange={handleRememberMeChange}
                      disabled={isSubmitting}
                    />
                    <Label htmlFor="rememberMe" className="text-sm text-muted-foreground">
                      Keep me signed in on this device
                    </Label>
                  </div>

                  <Button type="submit" className="w-full" disabled={isSubmitting}>
                    {renderSubmitContent()}
                  </Button>
                </form>

                <div className="mt-6 pt-6 border-t">
                  <p className="text-center text-sm text-muted-foreground">
                    Don't have an account?{" "}
                    <Button
                      type="button"
                      variant="link"
                      className="px-0"
                      onClick={() => navigate("/app/signup")}
                      disabled={isSubmitting}
                    >
                      Create an account
                    </Button>
                  </p>
                </div>
              </CardContent>
            </Card>

            <p className="mt-6 lg:mt-8 text-center text-xs text-muted-foreground">
              By signing in, you agree to our Terms of Service and Privacy Policy.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
