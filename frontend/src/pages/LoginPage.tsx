import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { login } from "@/app/auth";
import { loadUserSettings, resolveLandingPath } from "@/lib/user-settings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertCircle, ArrowRight, Shield, Zap, Clock } from "lucide-react";

type LoginForm = {
  username: string;
  password: string;
};

export default function LoginPage() {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    defaultValues: { username: "", password: "" },
  });

  const onSubmit = async (payload: LoginForm) => {
    setError("");
    setIsLoading(true);
    try {
      const me = await login(payload.username, payload.password);
      const settings = loadUserSettings();
      const landingPath = resolveLandingPath(settings, me.role);
      navigate(landingPath, { replace: true });
    } catch {
      setError("Invalid username or password. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const renderSubmitContent = () => {
    if (isLoading) {
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
              Enterprise Support,<br />Intelligently Automated
            </h1>
            <p className="mt-4 text-lg text-white/80 max-w-lg">
              Streamline your customer support with AI-powered ticket routing, SLA monitoring, and seamless team collaboration.
            </p>
          </div>

          <div className="grid sm:grid-cols-3 gap-4">
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Zap className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">AI-Powered Routing</p>
            </div>
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Shield className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">SLA Monitoring</p>
            </div>
            <div className="bg-white/10 backdrop-blur rounded-xl p-4">
              <Clock className="h-6 w-6 text-white mb-2" />
              <p className="text-sm font-medium text-white">24/7 Availability</p>
            </div>
          </div>
        </div>

        <div className="relative text-sm text-white/60">
          © 2024 SupportHub. All rights reserved.
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center justify-center mb-8">
            <div className="flex items-center gap-2">
              <div className="h-10 w-10 rounded-xl bg-blue-500 flex items-center justify-center">
                <Shield className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold">SupportHub</span>
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
              {error && (
                <div className="flex items-start gap-3 rounded-lg bg-destructive/10 p-4 mb-6">
                  <AlertCircle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
                  <div className="text-sm text-destructive">{error}</div>
                </div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="username">Username</Label>
                  <Input
                    id="username"
                    placeholder="Enter your username"
                    autoComplete="username"
                    autoFocus
                    {...register("username", { required: "Username is required" })}
                  />
                  {errors.username && (
                    <p className="text-sm text-destructive">{errors.username.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="password">Password</Label>
                    <Button variant="link" className="px-0 h-auto text-sm" type="button">
                      Forgot password?
                    </Button>
                  </div>
                  <Input
                    id="password"
                    type="password"
                    placeholder="Enter your password"
                    autoComplete="current-password"
                    {...register("password", { required: "Password is required" })}
                  />
                  {errors.password && (
                    <p className="text-sm text-destructive">{errors.password.message}</p>
                  )}
                </div>

                <Button type="submit" className="w-full" disabled={isLoading}>
                  {renderSubmitContent()}
                </Button>
              </form>

              <div className="mt-6 pt-6 border-t">
                <p className="text-center text-sm text-muted-foreground">
                  Don't have an account?{" "}
                  <Button variant="link" className="px-0" onClick={() => navigate("/app/signup")}>
                    Create an account
                  </Button>
                </p>
              </div>
            </CardContent>
          </Card>

          <p className="mt-8 text-center text-xs text-muted-foreground">
            By signing in, you agree to our Terms of Service and Privacy Policy.
          </p>
        </div>
      </div>
    </div>
  );
}
