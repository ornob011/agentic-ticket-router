PREVIOUS INVALID RESPONSE:
{failed_response}

ERROR MESSAGE:
{error_message}

INSTRUCTIONS:
The previous response was invalid. Please provide a valid JSON response with these fields:
- category: {category_values}
- priority: {priority_values}
- queue: {queue_values}
- next_action: {next_action_values}
- confidence: number between 0 and 1
- clarifying_question: string or null
- draft_reply: string or null
- rationale_tags: array of strings
- reasoning: brief explanation string

TICKET INFORMATION:
Ticket No: {ticket_no}
Subject: {subject}
Customer: {customer_name}
Initial Message: {initial_message}

Return ONLY valid JSON, no markdown, no explanation.
