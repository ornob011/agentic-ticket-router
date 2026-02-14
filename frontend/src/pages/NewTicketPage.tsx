import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { AlertCircle, Send, Sparkles } from "lucide-react";

export default function NewTicketPage() {
  const navigate = useNavigate();
  const [subject, setSubject] = useState("");
  const [content, setContent] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !content.trim()) {
      setError("Please fill in all fields");
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const response = await api.post("/tickets", { subject, content });
      navigate(`/app/tickets/${response.data.id}`);
    } catch {
      setError("Failed to create ticket. Please try again.");
      setSubmitting(false);
    }
  };

  const getSubmitLabel = () => {
    if (submitting) {
      return "Submitting...";
    }

    return "Submit Ticket";
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Create New Ticket"
        description="Submit a new support request"
      />

      <div className="mx-auto max-w-2xl space-y-6">
        <Card>
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
            <form onSubmit={onSubmit} className="space-y-4">
              {error && (
                <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  {error}
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="subject">Subject</Label>
                <Input
                  id="subject"
                  placeholder="Brief description of your issue"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="content">Description</Label>
                <Textarea
                  id="content"
                  placeholder="Please describe your issue in detail. Include any relevant error messages, steps to reproduce, or screenshots."
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  rows={8}
                  className="resize-none"
                />
                <p className="text-xs text-muted-foreground">
                  Tip: The more details you provide, the faster we can help you.
                </p>
              </div>

              <div className="flex gap-3 pt-2">
                <Button type="submit" disabled={submitting}>
                  <Send className="mr-2 h-4 w-4" />
                  {getSubmitLabel()}
                </Button>
                <Button type="button" variant="outline" onClick={() => navigate("/app/tickets")}>
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
