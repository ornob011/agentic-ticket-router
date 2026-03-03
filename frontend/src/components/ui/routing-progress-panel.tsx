import { useEffect, useMemo, useState } from "react";
import { Loader2, CheckCircle2, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { endpoints } from "@/lib/endpoints";
import { SSE_EVENT_NAME, SSE_EVENT_TYPE, type SseEventEnvelope } from "@/lib/sse-contract";

type AgentProgressEvent = {
  ticketId: number;
  node: "PLAN" | "SAFETY" | "TOOL_EXECUTION";
  status: "STARTED" | "COMPLETED";
  message: string;
  timestamp: string;
};

type StepEntry = {
  node: AgentProgressEvent["node"];
  status: AgentProgressEvent["status"];
  message: string;
};

const NODE_ORDER: AgentProgressEvent["node"][] = ["PLAN", "SAFETY", "TOOL_EXECUTION"];

const NODE_LABELS: Record<AgentProgressEvent["node"], string> = {
  PLAN: "Planning",
  SAFETY: "Safety Check",
  TOOL_EXECUTION: "Tool Execution",
};

function resolveStepMessage(message: string): string {
  return message;
}

function resolveConnectionLabel(
  isComplete: boolean,
  connectionState: "connecting" | "connected" | "reconnecting"
): string {
  if (isComplete) {
    return "Routing Complete";
  }

  if (connectionState === "connected") {
    return "Routing Stream Active";
  }

  if (connectionState === "reconnecting") {
    return "Reconnecting Updates...";
  }

  return "Connecting Updates...";
}

export function RoutingProgressPanel({
  ticketId,
  activationSeq,
}: {
  ticketId: number;
  activationSeq: number;
}) {
  const [steps, setSteps] = useState<StepEntry[]>([]);
  const [isComplete, setIsComplete] = useState(false);
  const [visible, setVisible] = useState(false);
  const [connectionState, setConnectionState] = useState<"connecting" | "connected" | "reconnecting">("connecting");

  useEffect(() => {
    const es = new EventSource(`/api/v1${endpoints.tickets.routingStream(ticketId)}`, { withCredentials: true });

    es.onopen = () => {
      setConnectionState("connected");
    };

    es.addEventListener(SSE_EVENT_NAME.CONNECTED, () => {
      setConnectionState("connected");
    });

    es.addEventListener(SSE_EVENT_NAME.HEARTBEAT, () => {
      setConnectionState("connected");
    });

    es.addEventListener(SSE_EVENT_NAME.EVENT, (e) => {
      const envelope = JSON.parse(e.data) as SseEventEnvelope<AgentProgressEvent>;
      if (envelope.eventType !== SSE_EVENT_TYPE.PROGRESS) {
        return;
      }

      const event = envelope.payload;
      if (!event) {
        return;
      }

      const message = resolveStepMessage(event.message);
      setVisible(true);
      setConnectionState("connected");
      if (event.node === "PLAN" && event.status === "STARTED") {
        setIsComplete(false);
      }
      setSteps((prev) => {
        if (event.node === "PLAN" && event.status === "STARTED") {
          return [{ node: event.node, status: event.status, message }];
        }

        const existing = prev.findIndex((s) => s.node === event.node);
        const entry: StepEntry = { node: event.node, status: event.status, message };
        if (existing >= 0) {
          const updated = [...prev];
          updated[existing] = entry;
          return updated;
        }
        return [...prev, entry];
      });
    });

    es.addEventListener(SSE_EVENT_NAME.COMPLETE, () => {
      setVisible(true);
      setIsComplete(true);
      setTimeout(() => setVisible(false), 3000);
    });

    es.addEventListener(SSE_EVENT_NAME.ERROR, () => {
      setConnectionState("reconnecting");
    });

    es.onerror = () => {
      setConnectionState("reconnecting");
    };

    return () => {
      es.close();
    };
  }, [ticketId]);

  useEffect(() => {
    if (activationSeq <= 0) {
      return;
    }
    setVisible(true);
    setIsComplete(false);
    setConnectionState("connecting");
    setSteps([]);
  }, [activationSeq]);

  const stepsByNode = useMemo(
    () => Object.fromEntries(steps.map((s) => [s.node, s])) as Partial<Record<AgentProgressEvent["node"], StepEntry>>,
    [steps]
  );
  const connectionLabel = resolveConnectionLabel(isComplete, connectionState);

  if (!visible) return null;

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-sm">
          {isComplete ? (
            <CheckCircle2 className="h-4 w-4 text-green-600" />
          ) : (
            <Loader2 className="h-4 w-4 animate-spin text-primary" />
          )}
          {connectionLabel}
          <Badge variant="info" className="ml-auto gap-1 text-xs">
            <Sparkles className="h-3 w-3" />
            AI Assisted
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2 pt-0">
        {NODE_ORDER.map((node) => {
          const step = stepsByNode[node];
          return (
            <div key={node} className="flex items-start gap-2 text-sm">
              <span className="mt-0.5 shrink-0">
                {!step ? (
                  <span className="inline-block h-4 w-4 rounded-full border-2 border-muted-foreground/30" />
                ) : step.status === "STARTED" ? (
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                ) : (
                  <CheckCircle2 className="h-4 w-4 text-green-600" />
                )}
              </span>
              <div className="min-w-0">
                <span className="font-medium">{NODE_LABELS[node]}</span>
                {step?.message && <p className="text-xs text-muted-foreground truncate">{step.message}</p>}
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
