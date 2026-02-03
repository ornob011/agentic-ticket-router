STRICT OUTPUT RULES:
1. Respond with ONLY a valid JSON object. No markdown formatting. No code blocks. No explanations.
2. Do NOT use markdown syntax like ```json or ```.
3. Return only the JSON object itself, nothing before or after it.
4. If you cannot determine a value, use null.
5. All arrays must be valid JSON arrays, even if empty []
6. All boolean values must be lowercase: true or false
7. Confidence scores must be decimal values between 0.0 and 1.0

TICKET INFORMATION:
- Ticket Number: {ticket_no}
- Subject: {subject}
- Customer: {customer_name} (Tier: {customer_tier})
- Initial Message: {initial_message}

CONVERSATION HISTORY:
{conversation_history}

ANALYSIS:
{analysis}

ROUTING INSTRUCTIONS:
Analyze the provided context including all available enum values. Make intelligent routing decisions using the actual enum values provided below.

Available Categories: {category}
Available Priorities: {priority}
Available Queues: {queue}
Available Next Actions: {next_action}

OUTPUT JSON STRUCTURE:
- category: one of the available categories
- priority: one of the available priorities
- queue: one of the available queues
- next_action: one of the available next actions
- confidence: decimal between 0.0 and 1.0
- clarifying_question: string or null (required if next_action is ASK_CLARIFYING)
- draft_reply: string or null (required if next_action is AUTO_REPLY)
- rationale_tags: array of 2-3 strings explaining the decision
- reasoning: brief explanation (20-50 words)

DECISION GUIDELINES:
- When category indicates SECURITY, consider next_action=ESCALATE
- When confidence is below 0.7, next_action must be HUMAN_REVIEW
- Consider customer tier when determining priority
- If conversation indicates customer frustration, consider HUMAN_REVIEW
- Use conversation history to detect repeated issues (may indicate escalation needed)
- Technical complexity suggests specialized queue assignment

OUTPUT: Return ONLY valid JSON. No markdown. No code blocks. No explanations.
