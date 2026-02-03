PREVIOUS INVALID RESPONSE:
{failed_response}

ERROR MESSAGE:
{error_message}

INSTRUCTIONS:
The previous response was invalid. Provide a valid JSON response with these fields:

OUTPUT JSON STRUCTURE (MUST MATCH EXACTLY):
{
  "category": "MUST be exactly one of: {category_values}",
  "priority": "MUST be exactly one of: {priority_values}",
  "queue": "MUST be exactly one of: {queue_values}",
  "next_action": "MUST be exactly one of: {next_action_values}",
  "confidence": "number between 0 and 1",
  "clarifying_question": "string or null",
  "draft_reply": "string or null",
  "rationale_tags": ["array of strings"]
}

TICKET INFORMATION:
- Ticket No: {ticket_no}
- Subject: {subject}
- Customer: {customer_name}
- Initial Message: {initial_message}

STRICT RULES:
- ALL enum values MUST match exactly - do not invent or combine values
- Example of INVALID: "MEDIUM-HIGH", "Technical", "tech_q", "SECURITY_Q_HIGH"
- Example of VALID: "MEDIUM", "HIGH", "TECHNICAL", "TECH_Q", "SECURITY_Q"
- Return ONLY JSON object, no markdown, no explanations

NEGATIVE EXAMPLES (DO NOT OUTPUT THESE):

INVALID (combined enum values):
{
  "priority": "MEDIUM-HIGH"
}

INVALID (wrong casing):
{
  "category": "billing"
}

INVALID (markdown):
```json
{
  "category": "TECHNICAL"
}
```

VALID OUTPUT EXAMPLE:
{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "queue": "TECH_Q",
  "next_action": "ASSIGN_QUEUE",
  "confidence": 0.85,
  "clarifying_question": null,
  "draft_reply": null,
  "rationale_tags": ["computer shutdown", "technical issue"]
}

Return ONLY valid JSON, no markdown, no explanation.
