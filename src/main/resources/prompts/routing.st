INPUT DATA:
{conversation_history}

**CRITICAL INSTRUCTION: READ THE LAST MESSAGE ONLY**
1. Scroll to the very bottom of the input.
2. If the last message is a **Question** (e.g., "How do I...", "Can I..."):
   - Set `next_action` to `AUTO_RESOLVE`.
   - Set `draft_reply` to a helpful answer.
   - **CRITICAL:** You MUST set `action_parameters` to `null`.
3. If the last message is a **Command** (e.g., "Change phone to..."):
   - Set `next_action` to `UPDATE_CUSTOMER_PROFILE`.
   - Set `draft_reply` to `null`.
   - Extract the value into `action_parameters`.

**ENUM VALUES:**
- Category: {category}
- Priority: {priority}
- Queue: {queue}
- Action: {next_action}

**RESPONSE TEMPLATE:**
\{
  "internal_note": "Analyzing Last Message: [INSERT_LAST_MESSAGE]. Is it a Question? [YES/NO]",
  "category": "SELECT_FROM_ENUMS",
  "priority": "SELECT_FROM_ENUMS",
  "queue": "SELECT_FROM_ENUMS",
  "next_action": "SELECT_FROM_ENUMS",
  "confidence": 1.0,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
      "phone_number": null,
      "company_name": null,
      "address": null,
      "city": null,
      "postal_code": null,
      "preferred_language_code": null
  \},
  "rationale_tags": ["latest_message"]
\}

**OVERRIDE RULES:**
1. If `internal_note` says "Is it a Question? YES", then `action_parameters` MUST be `null`.
2. If `internal_note` says "Is it a Question? NO", then replace the `null` in `action_parameters` with the value from the text.
