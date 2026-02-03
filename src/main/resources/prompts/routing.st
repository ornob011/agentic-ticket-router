CRITICAL OUTPUT RULES - ANY VIOLATION WILL CAUSE FAILURE:
1. Respond with ONLY a valid JSON object. No markdown formatting. No code blocks. No explanations.
2. Do NOT use markdown syntax like ```json or ```.
3. Return only the JSON object itself, nothing before or after it.
4. If you cannot determine a value, use null.
5. All arrays must be valid JSON arrays, even if empty []
6. All boolean values must be lowercase: true or false
7. Confidence scores must be decimal values between 0.0 and 1.0
8. ALL enum values MUST match exactly. Do not invent, combine, or modify values.

TICKET INFORMATION:
- Ticket Number: {ticket_no}
- Subject: {subject}
- Customer: {customer_name} (Tier: {customer_tier})
- Initial Message: {initial_message}

CONVERSATION HISTORY:
{conversation_history}

ANALYSIS:
{analysis}

AVAILABLE ENUM VALUES (USE EXACTLY AS SHOWN):
Category options: {category}
Priority options: {priority}
Queue options: {queue}
Next action options: {next_action}

OUTPUT JSON STRUCTURE (MUST MATCH EXACTLY):
\{
  "category": "string - use one of the category options above",
  "priority": "string - use one of the priority options above",
  "queue": "string - use one of the queue options above",
  "next_action": "string - use one of the next action options above",
  "confidence": "decimal between 0.0 and 1.0",
  "clarifying_question": "string or null (required if next_action is ASK_CLARIFYING)",
  "draft_reply": "string or null (required if next_action is AUTO_REPLY)",
  "rationale_tags": ["array of 2-3 strings explaining the decision"]
\}

DECISION GUIDELINES:
- When category indicates SECURITY, consider next_action=ESCALATE
- When confidence is below 0.7, next_action must be HUMAN_REVIEW
- Consider customer tier when determining priority
- If conversation indicates customer frustration, consider HUMAN_REVIEW
- Use conversation history to detect repeated issues (may indicate escalation needed)
- Technical complexity suggests specialized queue assignment

STRICT RULES - READ CAREFULLY:
- DO NOT invent or combine enum values
  - INVALID: "MEDIUM-HIGH", "Technical", "tech_q", "SECURITY_Q_HIGH"
  - VALID: "MEDIUM", "HIGH", "TECHNICAL", "TECH_Q", "SECURITY_Q"
- DO NOT add extra fields beyond the 8 specified in the JSON structure
- DO NOT include markdown formatting around the JSON
- DO NOT add explanatory text before or after the JSON
- ALL enum values MUST match exactly (case-sensitive, hyphens/underscores matter)

NEGATIVE EXAMPLES (DO NOT OUTPUT THESE):

INVALID (combined enum values):
\{
  "priority": "MEDIUM-HIGH"
\}

INVALID (wrong casing):
\{
  "category": "technical"
\}

INVALID (extra field):
\{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "queue": "TECH_Q",
  "next_action": "ASSIGN_QUEUE",
  "confidence": 0.85,
  "reasoning": "This is a technical issue"
\}

INVALID (markdown formatting):
```json
\{
  "category": "TECHNICAL"
\}
```

INVALID (explanatory text):
The ticket analysis suggests:
\{
  "category": "TECHNICAL"
\}

VALID OUTPUT EXAMPLE:
\{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "queue": "TECH_Q",
  "next_action": "ASSIGN_QUEUE",
  "confidence": 0.85,
  "clarifying_question": null,
  "draft_reply": null,
  "rationale_tags": ["computer shutdown", "technical issue", "hardware problem"]
\}

OUTPUT: Return ONLY valid JSON. No markdown. No code blocks. No explanations.
