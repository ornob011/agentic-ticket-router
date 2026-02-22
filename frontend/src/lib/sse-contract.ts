export const SSE_EVENT_NAME = {
  CONNECTED: "sse-connected",
  HEARTBEAT: "sse-heartbeat",
  EVENT: "sse-event",
  COMPLETE: "sse-complete",
  ERROR: "sse-error",
} as const;

export const SSE_CHANNEL = {
  ROUTING_PROGRESS: "ROUTING_PROGRESS",
  DRAFT_REPLY: "DRAFT_REPLY",
} as const;

export const SSE_EVENT_TYPE = {
  CONNECTED: "CONNECTED",
  HEARTBEAT: "HEARTBEAT",
  PROGRESS: "PROGRESS",
  TOKEN: "TOKEN",
  DONE: "DONE",
  COMPLETE: "COMPLETE",
  ERROR: "ERROR",
} as const;

export type SseEventName = (typeof SSE_EVENT_NAME)[keyof typeof SSE_EVENT_NAME];
export type SseEventType = (typeof SSE_EVENT_TYPE)[keyof typeof SSE_EVENT_TYPE];
export type SseChannel = (typeof SSE_CHANNEL)[keyof typeof SSE_CHANNEL];

export type SseEventEnvelope<TPayload = unknown> = {
  channel: SseChannel;
  eventName: SseEventName;
  eventType: SseEventType;
  streamId: string;
  resourceId: string;
  timestamp: string;
  payload: TPayload;
};
