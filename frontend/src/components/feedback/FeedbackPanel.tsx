import { useEffect, useMemo, useState } from "react";
import { Star, AlertTriangle, Loader2, CheckCircle2, XCircle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { getMe } from "@/app/auth";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  useSubmitRatingMutation,
  useSubmitCorrectionMutation,
  useSubmitApprovalMutation,
  useSubmitRejectionMutation,
  useTicketFeedback,
} from "@/lib/hooks";
import { NEXT_ACTION_OPTIONS } from "@/lib/next-actions";
import type { FeedbackType } from "@/lib/api";

type FeedbackPanelProps = {
  ticketId: number;
  routingId?: number;
  originalCategory?: string;
  originalAction?: string;
  onFeedbackSubmitted?: () => void;
};

const RATING_LABELS = ["", "Poor", "Fair", "Good", "Very Good", "Excellent"];
const DEFAULT_CORRECTED_ACTION = "HUMAN_REVIEW";

function FeedbackPanel({
  ticketId,
  routingId,
  originalCategory,
  originalAction,
  onFeedbackSubmitted,
}: FeedbackPanelProps) {
  const [activeType, setActiveType] = useState<FeedbackType | null>(null);
  const [rating, setRating] = useState<number>(0);
  const [hoveredRating, setHoveredRating] = useState<number>(0);
  const [ratingNotes, setRatingNotes] = useState<string>("");
  const [actionNotes, setActionNotes] = useState<string>("");
  const [correctedAction, setCorrectedAction] = useState<string>(DEFAULT_CORRECTED_ACTION);

  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });
  const { feedbackItems, reloadFeedback } = useTicketFeedback(ticketId);

  const isCustomer = me?.role === "CUSTOMER";
  const canUseActionFeedback = !isCustomer && !!originalAction;
  const availableFeedbackTypes: FeedbackType[] = canUseActionFeedback
    ? ["RATING", "APPROVAL", "REJECTION", "CORRECTION"]
    : ["RATING"];

  const userDisplayName = me?.fullName || me?.username;
  const latestMyRating = useMemo(
    () => feedbackItems.find((item) => item.feedbackType === "RATING" && item.agentName === userDisplayName),
    [feedbackItems, userDisplayName]
  );

  const ratingMutation = useSubmitRatingMutation();
  const correctionMutation = useSubmitCorrectionMutation();
  const approvalMutation = useSubmitApprovalMutation();
  const rejectionMutation = useSubmitRejectionMutation();

  useEffect(() => {
    if (!latestMyRating) {
      return;
    }

    setRating(latestMyRating.rating ?? 0);
    setRatingNotes(latestMyRating.notes ?? "");
  }, [latestMyRating]);

  const isActionPending = correctionMutation.isPending || approvalMutation.isPending || rejectionMutation.isPending;

  const resetActionForm = () => {
    setActionNotes("");
    setCorrectedAction(DEFAULT_CORRECTED_ACTION);
    setActiveType(null);
  };

  const afterFeedbackSubmit = async () => {
    await reloadFeedback();
    onFeedbackSubmitted?.();
  };

  const handleRatingSubmit = () => {
    if (rating === 0) {
      return;
    }

    ratingMutation.mutate(
      { ticketId, routingId, rating, notes: ratingNotes || undefined },
      {
        onSuccess: () => {
          setActiveType(null);
          void afterFeedbackSubmit();
        },
      }
    );
  };

  const handleApprovalSubmit = () => {
    if (!originalAction || !routingId) {
      return;
    }

    approvalMutation.mutate(
      {
        ticketId,
        routingId,
        originalCategory,
        originalAction,
        notes: actionNotes || undefined,
      },
      {
        onSuccess: () => {
          resetActionForm();
          void afterFeedbackSubmit();
        },
      }
    );
  };

  const handleRejectionSubmit = () => {
    if (!originalAction || !routingId) {
      return;
    }

    rejectionMutation.mutate(
      {
        ticketId,
        routingId,
        originalAction,
        notes: actionNotes || undefined,
      },
      {
        onSuccess: () => {
          resetActionForm();
          void afterFeedbackSubmit();
        },
      }
    );
  };

  const handleCorrectionSubmit = () => {
    if (!originalAction) {
      return;
    }

    correctionMutation.mutate(
      {
        ticketId,
        routingId,
        originalCategory,
        originalAction,
        correctedAction,
        notes: actionNotes || undefined,
      },
      {
        onSuccess: () => {
          resetActionForm();
          void afterFeedbackSubmit();
        },
      }
    );
  };

  const displayRating = hoveredRating || rating;
  const requiresRoutingId = activeType === "APPROVAL" || activeType === "REJECTION";
  const missingRoutingId = requiresRoutingId && !routingId;

  return (
    <div className="space-y-4">
      {!activeType && (
        <div className="space-y-3">
          {latestMyRating?.rating ? (
            <div className="inline-flex items-center gap-2 rounded-md bg-primary/10 px-3 py-1.5">
              <div className="flex items-center gap-0.5">
                {[1, 2, 3, 4, 5].map((star) => (
                  <Star
                    key={star}
                    className={`h-3.5 w-3.5 ${
                      star <= (latestMyRating.rating ?? 0) ? "fill-primary text-primary" : "text-primary/30"
                    }`}
                  />
                ))}
              </div>
              <span className="text-sm font-medium text-primary">Your rating: {latestMyRating.rating}/5</span>
            </div>
          ) : null}

          {canUseActionFeedback ? (
            <p className="text-sm text-muted-foreground">
              Approval and correction contribute to routing pattern learning. Rating evaluates quality.
            </p>
          ) : (
            <p className="text-sm text-muted-foreground">Pattern-learning feedback requires an AI routing action.</p>
          )}

          <div className="flex flex-wrap gap-2">
            {availableFeedbackTypes.map((type) => (
              <Button
                key={type}
                variant="outline"
                size="sm"
                onClick={() => setActiveType(type)}
                className="flex items-center gap-1.5 border-primary/20 bg-primary/5 hover:border-primary/30 hover:bg-primary/10"
              >
                {type === "RATING" ? <Star className="h-4 w-4 text-primary" /> : null}
                {type === "CORRECTION" ? <AlertTriangle className="h-4 w-4 text-primary" /> : null}
                {type === "APPROVAL" ? <CheckCircle2 className="h-4 w-4 text-primary" /> : null}
                {type === "REJECTION" ? <XCircle className="h-4 w-4 text-primary" /> : null}
                {type === "RATING" ? "Rate" : null}
                {type === "CORRECTION" ? "Correct" : null}
                {type === "APPROVAL" ? "Approve" : null}
                {type === "REJECTION" ? "Reject" : null}
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
                    star <= displayRating ? "fill-primary text-primary" : "text-primary/30 hover:text-primary/50"
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
                <span className="text-sm text-muted-foreground">Click to rate</span>
              )}
            </div>
          </div>
          <Textarea
            placeholder="Optional notes about this rating..."
            value={ratingNotes}
            onChange={(event) => setRatingNotes(event.target.value)}
            rows={2}
            className="resize-none text-sm"
          />
          <div className="flex gap-2">
            <Button size="sm" onClick={handleRatingSubmit} disabled={rating === 0 || ratingMutation.isPending}>
              {ratingMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
              Submit Rating
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setActiveType(null);
                setRating(0);
                setHoveredRating(0);
                setRatingNotes("");
              }}
            >
              Cancel
            </Button>
          </div>
        </div>
      )}

      {(activeType === "APPROVAL" || activeType === "REJECTION" || activeType === "CORRECTION") && (
        <div className="space-y-3">
          {activeType === "CORRECTION" ? (
            <div className="space-y-2">
              <p className="text-sm font-medium">Corrected action</p>
              <Select value={correctedAction} onValueChange={setCorrectedAction}>
                <SelectTrigger>
                  <SelectValue placeholder="Select corrected action" />
                </SelectTrigger>
                <SelectContent>
                  {NEXT_ACTION_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          ) : null}

          <Textarea
            placeholder="Optional notes..."
            value={actionNotes}
            onChange={(event) => setActionNotes(event.target.value)}
            rows={2}
            className="resize-none text-sm"
          />

          {missingRoutingId ? (
            <p className="text-sm text-destructive">Routing id is required for this feedback action.</p>
          ) : null}

          <div className="flex gap-2">
            {activeType === "APPROVAL" ? (
              <Button size="sm" onClick={handleApprovalSubmit} disabled={isActionPending || missingRoutingId}>
                {isActionPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Submit Approval
              </Button>
            ) : null}

            {activeType === "REJECTION" ? (
              <Button size="sm" onClick={handleRejectionSubmit} disabled={isActionPending || missingRoutingId}>
                {isActionPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Submit Rejection
              </Button>
            ) : null}

            {activeType === "CORRECTION" ? (
              <Button size="sm" onClick={handleCorrectionSubmit} disabled={isActionPending || !correctedAction}>
                {isActionPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Submit Correction
              </Button>
            ) : null}

            <Button size="sm" variant="ghost" onClick={resetActionForm}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export { FeedbackPanel };
