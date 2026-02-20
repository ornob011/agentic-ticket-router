PREVIOUS INVALID RESPONSE:
{failed_response}

ERROR MESSAGE:
{error_message}

AVAILABLE ENUM VALUES (USE EXACTLY AS SHOWN):
Category options: {category_values}
Priority options: {priority_values}
Queue options: {queue_values}
Next action options: {next_action_values}

OUTPUT JSON STRUCTURE (MUST MATCH EXACTLY):
\{
  "internal_note": "string or null",
  "category": "copy one exact category option",
  "priority": "copy one exact priority option",
  "queue": "copy one exact queue option",
  "next_action": "copy one exact next action option",
  "confidence": 0.0,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "article_id": null,
    "phone_number": null,
    "company_name": null,
    "address": null,
    "city": null,
    "postal_code": null,
    "preferred_language_code": null
  \},
  "rationale_tags": ["array of strings"]
\}

TICKET INFORMATION:
- Ticket No: {ticket_no}
- Subject: {subject}
- Customer: {customer_name}
- Initial Message: {initial_message}

STRICT RULES:
- ALL enum values MUST match exactly - do not invent, combine, transform, abbreviate, or change casing.
- Return ONLY JSON object, no markdown, no explanations
- Extract values from the ACTUAL ticket content
- Do NOT use example-based responses
- Keep `rationale_tags` evidence-based and do not default to "latest_message" unless it is explicitly justified by the ticket content
- Queue/category pair MUST follow this policy:
{routing_policy}
- Ensure action contracts:
  - If next_action is `AUTO_REPLY` or `AUTO_RESOLVE`, draft_reply must be non-null and actionable.
  - If next_action is `ASK_CLARIFYING`, clarifying_question must be non-null and draft_reply must be null.
  - If next_action is `ASSIGN_QUEUE`, clarifying_question and draft_reply must be null.
  - If next_action is `USE_KNOWLEDGE_ARTICLE`, action_parameters.article_id must be positive.
  - If next_action is `UPDATE_CUSTOMER_PROFILE`, include at least one concrete profile field value in action_parameters.

Return ONLY valid JSON, no markdown, no explanation.
