PREVIOUS INVALID RESPONSE:
{failedResponse}

ERROR MESSAGE:
{errorMessage}

INSTRUCTIONS:
The previous response was invalid. Please provide a valid JSON response with these fields:
- category: {categoryValues}
- priority: {priorityValues}
- queue: {queueValues}
- next_action: {nextActionValues}
- confidence: number between 0 and 1
- clarifying_question: string or null
- draft_reply: string or null
- rationale_tags: array of strings
- reasoning: brief explanation string

TICKET INFORMATION:
Ticket No: {ticketNo}
Subject: {subject}
Customer: {customerName}
Initial Message: {initialMessage}

Return ONLY valid JSON, no markdown, no explanation.
