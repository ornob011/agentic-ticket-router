import { useEffect, useMemo, useState } from "react";
import { Star, AlertTriangle, Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { getMe } from "@/app/auth";
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

const RATING_LABELS = ["", "Poor", "Fair", "Good", "Very Good", "Excellent"];

function FeedbackPanel({
  ticketId,
  originalCategory,
  originalAction,
  onFeedbackSubmitted,
}: FeedbackPanelProps) {
  const [activeType, setActiveType] = useState<FeedbackType | null>(null);
  const [rating, setRating] = useState<number>(0);
  const [hoveredRating, setHoveredRating] = useState<number>(0);
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

  const displayRating = hoveredRating || rating;

  return (
    <div className="space-y-4">
      {!activeType && !showCorrectionForm && (
        <div className="space-y-3">
          {latestMyRating?.rating ? (
            <div className="inline-flex items-center gap-2 rounded-md bg-primary/10 px-3 py-1.5">
              <div className="flex items-center gap-0.5">
                {[1, 2, 3, 4, 5].map((star) => (
                  <Star
                    key={star}
                    className={`h-3.5 w-3.5 ${
                      star <= (latestMyRating.rating ?? 0)
                        ? "fill-primary text-primary"
                        : "text-primary/30"
                    }`}
                  />
                ))}
              </div>
              <span className="text-sm font-medium text-primary">
                Your rating: {latestMyRating.rating}/5
              </span>
            </div>
          ) : null}
          <div className="flex flex-wrap gap-2">
            {availableFeedbackTypes.map((type) => (
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
                className="flex items-center gap-1.5 bg-primary/5 hover:bg-primary/10 border-primary/20 hover:border-primary/30"
              >
                {type === "RATING" ? (
                  <Star className="h-4 w-4 text-primary" />
                ) : (
                  <AlertTriangle className="h-4 w-4 text-primary" />
                )}
                {type === "RATING" ? "Rate" : "Correct"}
              </Button>
            ))}
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
                onMouseEnter={() => setHoveredRating(star)}
                onMouseLeave={() => setHoveredRating(0)}
                className="rounded-sm p-1 transition-transform hover:scale-110"
              >
                <Star
                  className={`h-6 w-6 transition-colors ${
                    star <= displayRating
                      ? "fill-primary text-primary"
                      : "text-primary/30 hover:text-primary/50"
                  }`}
                />
              </button>
            ))}
            <div className="ml-3">
              {rating > 0 ? (
                <span className="text-sm font-medium text-primary">
                  {rating}/5 - {RATING_LABELS[rating]}
                </span>
              ) : (
                <span className="text-sm text-muted-foreground">
                  Click to rate
                </span>
              )}
            </div>
          </div>
          <Textarea
            placeholder="Optional notes about this rating..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            className="text-sm resize-none"
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={handleRatingSubmit}
              disabled={rating === 0 || ratingMutation.isPending}
            >
              {ratingMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Submit Rating
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setActiveType(null);
                setRating(0);
                setHoveredRating(0);
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
          <div className="rounded-md bg-primary/10 px-3 py-2">
            <p className="text-sm text-primary">
              Mark this ticket for human review if the AI decision was incorrect.
            </p>
          </div>
          <Textarea
            placeholder="Explain why this decision needs correction..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            className="text-sm resize-none"
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={handleCorrectionSubmit}
              disabled={correctionMutation.isPending}
            >
              {correctionMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
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

export { FeedbackPanel };
