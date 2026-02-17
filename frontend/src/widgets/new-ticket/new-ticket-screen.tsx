import type { FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { AlertCircle, Send, Sparkles, Lightbulb, Clock, MessageSquare, FileText, Zap } from "lucide-react";

type NewTicketScreenProps = Readonly<{
  subject: string;
  content: string;
  validationError: string;
  isSubmitting: boolean;
  onSubjectChange: (value: string) => void;
  onContentChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onCancel: () => void;
}>;

export function NewTicketScreen({
  subject,
  content,
  validationError,
  isSubmitting,
  onSubjectChange,
  onContentChange,
  onSubmit,
  onCancel,
}: NewTicketScreenProps) {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Create New Ticket"
        description="Submit a new support request"
      />

      <div className="grid gap-6 lg:grid-cols-3 lg:items-stretch">
        <div className="lg:col-span-2">
          <Card className="h-full">
            <CardHeader>
              <CardTitle>Ticket Details</CardTitle>
              <CardDescription className="flex flex-wrap items-center gap-x-1">
                Describe your issue or request in detail.
                {" "}
                <span className="inline-flex items-center gap-1 text-primary">
                  <Sparkles className="h-3 w-3" />
                  AI-powered routing
                </span>
                {" "}
                will categorize and route automatically.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={(event) => void onSubmit(event)} className="space-y-4">
                {validationError && (
                  <div className="flex items-start gap-3 rounded-lg bg-destructive/10 p-4 mb-2" role="alert">
                    <AlertCircle className="h-5 w-5 shrink-0 text-destructive" />
                    <p className="text-sm text-destructive">{validationError}</p>
                  </div>
                )}

                <div className="space-y-2">
                  <Label htmlFor="subject">Subject</Label>
                  <Input
                    id="subject"
                    placeholder="Brief description of your issue"
                    value={subject}
                    onChange={(event) => onSubjectChange(event.target.value)}
                    disabled={isSubmitting}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="content">Description</Label>
                  <Textarea
                    id="content"
                    placeholder="Please describe your issue in detail. Include any relevant error messages, steps to reproduce, or screenshots."
                    value={content}
                    onChange={(event) => onContentChange(event.target.value)}
                    rows={10}
                    className="resize-none"
                    disabled={isSubmitting}
                  />
                </div>

                <div className="flex flex-col gap-3 pt-2 sm:flex-row">
                  <Button type="submit" disabled={isSubmitting}>
                    <Send className="mr-2 h-4 w-4" />
                    {isSubmitting ? "Submitting..." : "Submit Ticket"}
                  </Button>
                  <Button type="button" variant="outline" onClick={onCancel} disabled={isSubmitting}>
                    Cancel
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="flex flex-col gap-4">
          <Card className="flex-1">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Lightbulb className="h-4 w-4 text-primary" />
                Writing Tips
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-3">
                <FileText className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="space-y-1">
                  <p className="text-sm font-medium">Be Specific</p>
                  <p className="text-xs text-muted-foreground">
                    Include exact error messages and steps to reproduce the issue.
                  </p>
                </div>
              </div>
              <div className="flex gap-3">
                <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="space-y-1">
                  <p className="text-sm font-medium">One Issue Per Ticket</p>
                  <p className="text-xs text-muted-foreground">
                    Create separate tickets for unrelated issues.
                  </p>
                </div>
              </div>
              <div className="flex gap-3">
                <Clock className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="space-y-1">
                  <p className="text-sm font-medium">Mention Urgency</p>
                  <p className="text-xs text-muted-foreground">
                    Let us know if this is blocking your work.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="gradient-ai flex-1">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Zap className="h-4 w-4 text-primary" />
                AI-Powered Routing
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-muted-foreground">
                Our system automatically:
              </p>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
                  <span>Understands your issue and assigns the right category</span>
                </li>
                <li className="flex items-start gap-2">
                  <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
                  <span>Sets priority based on urgency and impact</span>
                </li>
                <li className="flex items-start gap-2">
                  <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
                  <span>Routes to the best team for faster resolution</span>
                </li>
                <li className="flex items-start gap-2">
                  <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-primary" />
                  <span>Provides an instant AI response when possible</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
