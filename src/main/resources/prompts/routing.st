STRICT OUTPUT RULES:
1. Respond with ONLY a valid JSON object. No markdown formatting. No code blocks. No explanations.
2. Do NOT use markdown syntax like ```json or ```.
3. Return only the JSON object itself, nothing before or after it.
4. If you cannot determine a value, use null.
5. All arrays must be valid JSON arrays, even if empty []
6. All boolean values must be lowercase: true or false
7. Confidence scores must be decimal values between 0.0 and 1.0

TICKET INFORMATION:
- Ticket Number: {ticketNo}
- Subject: {subject}
- Customer: {customerName} (Tier: {customerTier})
- Initial Message: {initialMessage}

CONVERSATION HISTORY:
{conversationHistory}

ANALYSIS:
{analysis}

ROUTING INSTRUCTIONS:
Analyze the provided context including all available enum values. Make intelligent routing decisions using the actual enum values provided below.

Available Categories: {category}
Available Priorities: {priority}
Available Queues: {queue}
Available Next Actions: {next_action}

Return JSON with:
{
  "category": "Choose appropriate category from available categories based on analysis",
  "priority": "Choose appropriate priority from available priorities based on urgency",
  "queue": "Assign to the most suitable queue from available queues based on category",
  "next_action": "Select appropriate action from available next actions",
  "confidence": 0.0 to 1.0,
  "clarifying_question": "Required if next_action is ASK_CLARIFYING, otherwise null",
  "draft_reply": "Required if next_action is AUTO_REPLY, otherwise null",
  "rationale_tags": ["Extract 2-3 key tags that explain the decision"],
  "reasoning": "Brief explanation (20-50 words) of why this routing decision was chosen"
}

DECISION GUIDELINES:
- When category indicates SECURITY (from available categories), consider next_action=ESCALATE
- When confidence is below 0.7, next_action must be HUMAN_REVIEW
- Consider customer tier when determining priority (PREMIUM may warrant higher priority)
- If conversation indicates customer frustration, consider HUMAN_REVIEW
- Use conversation history to detect repeated issues (may indicate escalation needed)
- Technical complexity suggests specialized queue assignment
- Rationale should be concise and explain key factors influencing your decision

OUTPUT: Return ONLY valid JSON. No markdown. No code blocks. No explanations.
