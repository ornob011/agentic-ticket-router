import { useEffect, useMemo, useState } from "react";
import { Star, AlertTriangle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { getMe } from "@/app/auth";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useSubmitRatingMutation, useSubmitCorrectionMutation, useTicketFeedback } from "@/lib/hooks";
import type { FeedbackType } from "@/lib/api";

type FeedbackPanelProps = {
  ticketId: number;
  originalCategory?: string;
  originalAction?: string;
  onFeedbackSubmitted?: () => void;
};

const feedbackTypeConfig: Record<"RATING" | "CORRECTION", { label: string; icon: typeof Star; color: string }> = {
  RATING: { label: "Rate", icon: Star, color: "text-yellow-500" },
  CORRECTION: { label: "Correct", icon: AlertTriangle, color: "text-orange-500" },
};

export function FeedbackPanel({
  ticketId,
  originalCategory,
  originalAction,
  onFeedbackSubmitted,
}: FeedbackPanelProps) {
  const [activeType, setActiveType] = useState<FeedbackType | null>(null);
  const [rating, setRating] = useState<number>(0);
  const [notes, setNotes] = useState<string>("");
  const [showCorrectionForm, setShowCorrectionForm] = useState(false);
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });
  const { feedbackItems, reloadFeedback } = useTicketFeedback(ticketId);
  const isCustomer = me?.role === "CUSTOMER";
  const availableFeedbackTypes = isCustomer || !originalAction
    ? (["RATING"] as const)
    : (["RATING", "CORRECTION"] as const);
  const userDisplayName = me?.fullName || me?.username;
  const latestMyRating = useMemo(() => feedbackItems.find((item) =>
    item.feedbackType === "RATING" && item.agentName === userDisplayName
  ), [feedbackItems, userDisplayName]);

  const ratingMutation = useSubmitRatingMutation();
  const correctionMutation = useSubmitCorrectionMutation();

  useEffect(() => {
    if (!latestMyRating) {
      return;
    }

    setRating(latestMyRating.rating ?? 0);
    setNotes(latestMyRating.notes ?? "");
  }, [latestMyRating]);

  const handleRatingSubmit = () => {
    if (rating === 0) return;
    ratingMutation.mutate(
      { ticketId, rating, notes: notes || undefined },
      {
        onSuccess: () => {
          setActiveType(null);
          void reloadFeedback();
          onFeedbackSubmitted?.();
        },
      }
    );
  };

  const handleCorrectionSubmit = () => {
    if (!originalAction) return;
    correctionMutation.mutate(
      {
        ticketId,
        originalCategory,
        originalAction,
        correctedAction: "HUMAN_REVIEW",
        notes: notes || undefined,
      },
      {
        onSuccess: () => {
          setShowCorrectionForm(false);
          setNotes("");
          void reloadFeedback();
          onFeedbackSubmitted?.();
        },
      }
    );
  };

  return (
    <div className="rounded-lg border bg-card p-4">
      <div className="mb-3 flex items-center justify-between">
        <h4 className="text-sm font-medium text-foreground">
          AI Decision Feedback
        </h4>
        <Badge variant="info" className="text-xs">
          Help improve routing
        </Badge>
      </div>

      {!activeType && !showCorrectionForm && (
        <div className="space-y-2">
          {latestMyRating?.rating ? (
            <p className="text-xs text-muted-foreground">
              Your rating: {latestMyRating.rating}/5
            </p>
          ) : null}
          <div className="flex flex-wrap gap-2">
            {availableFeedbackTypes.map((type) => {
              const config = feedbackTypeConfig[type];
              const Icon = config.icon;
              return (
                <Button
                  key={type}
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    if (type === "RATING") {
                      setActiveType("RATING");
                    } else {
                      setShowCorrectionForm(true);
                    }
                  }}
                  className="flex items-center gap-1"
                >
                  <Icon className={`h-4 w-4 ${config.color}`} />
                  {config.label}
                </Button>
              );
            })}
          </div>
        </div>
      )}

      {activeType === "RATING" && (
        <div className="space-y-3">
          <div className="flex items-center gap-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                onClick={() => setRating(star)}
                className="rounded-sm p-1 transition-transform hover:scale-110"
              >
                <Star
                  className={`h-6 w-6 ${
                    star <= rating
                      ? "fill-yellow-400 text-yellow-400"
                      : "text-muted-foreground/40"
                  }`}
                />
              </button>
            ))}
            <span className="ml-2 text-sm text-muted-foreground">
              {rating > 0 ? `${rating}/5` : "Select rating"}
            </span>
          </div>
          <Textarea
            placeholder="Optional notes about this rating..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            className="text-sm"
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={handleRatingSubmit}
              disabled={rating === 0 || ratingMutation.isPending}
            >
              Submit Rating
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setActiveType(null);
                setRating(0);
                setNotes("");
              }}
            >
              Cancel
            </Button>
          </div>
        </div>
      )}

      {showCorrectionForm && (
        <div className="space-y-3">
          <p className="text-sm text-muted-foreground">
            Mark this ticket for human review if the AI decision was incorrect.
          </p>
          <Textarea
            placeholder="Explain why this decision needs correction..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            className="text-sm"
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={handleCorrectionSubmit}
              disabled={correctionMutation.isPending}
            >
              Submit Correction
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setShowCorrectionForm(false);
                setNotes("");
              }}
            >
              Cancel
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
