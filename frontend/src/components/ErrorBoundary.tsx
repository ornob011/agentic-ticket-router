import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { AlertTriangle, RefreshCw, Home } from "lucide-react";

type Props = {
  children: ReactNode;
  fallback?: ReactNode;
};

type ErrorBoundaryClassProps = Props & {
  onGoHome: () => void;
};

type State = {
  hasError: boolean;
  error: Error | null;
};

class ErrorBoundaryImpl extends Component<ErrorBoundaryClassProps, State> {
  constructor(props: ErrorBoundaryClassProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error("ErrorBoundary caught an error:", error, errorInfo);
  }

  handleRetry = (): void => {
    this.setState({ hasError: false, error: null });
  };

  handleGoHome = (): void => {
    this.props.onGoHome();
  };

  render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
          <Card className="w-full max-w-lg">
            <CardContent className="p-8">
              <div className="flex flex-col items-center text-center space-y-6">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
                  <AlertTriangle className="h-8 w-8 text-destructive" />
                </div>

                <div className="space-y-2">
                  <h1 className="text-2xl font-semibold text-foreground">
                    Something went wrong
                  </h1>
                  <p className="text-muted-foreground max-w-md">
                    An unexpected error occurred. Please try again or return to the dashboard.
                  </p>
                </div>

                {process.env.NODE_ENV === "development" && this.state.error && (
                  <div className="w-full text-left bg-muted rounded-lg p-4 overflow-auto max-h-48">
                    <p className="text-xs font-mono text-destructive break-all">
                      {this.state.error.message}
                    </p>
                  </div>
                )}

                <div className="flex flex-wrap gap-3 pt-2">
                  <Button variant="outline" onClick={this.handleRetry}>
                    <RefreshCw className="h-4 w-4 mr-2" />
                    Try Again
                  </Button>
                  <Button onClick={this.handleGoHome}>
                    <Home className="h-4 w-4 mr-2" />
                    Dashboard
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}

export function ErrorBoundary(props: Readonly<Props>) {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate("/app/dashboard");
  };

  return (
    <ErrorBoundaryImpl
      {...props}
      onGoHome={handleGoHome}
    />
  );
}
