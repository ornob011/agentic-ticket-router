INPUTS:
- History: {conversation_history}
- Knowledge: {relevant_articles}

**PRIORITY CHECKLIST (Execute in Order):**

1. **CHECK FOR COMMANDS (Highest Priority):**
   - Does the LAST message say "Change/Update [Field] to [Value]"?
   - **YES:**
     - Action: `UPDATE_CUSTOMER_PROFILE`
     - Extraction: You **MUST** extract the value into `action_parameters`.
     - Draft Reply: `null`.
     - **STOP HERE.** (Do not check knowledge).

2. **CHECK FOR QUESTIONS (If not a command):**
   - Does the LAST message ask "How to...", "Can I...", or describe an issue?
   - **YES:**
     - **Check Knowledge**: Read `{relevant_articles}`.
     - If Knowledge Found -> Action: `AUTO_RESOLVE`. Write a `draft_reply` using the article.
     - If Knowledge Empty -> Action: `ASSIGN_QUEUE`. Set `draft_reply` to `null` (Do NOT hallucinate).

3. **CHECK FOR VAGUE INPUT:**
   - If unclear -> Action: `ASK_CLARIFYING`. Write a `clarifying_question`.

**CONFIDENCE CALCULATION:**
- If Request is Clear/Simple (Command or Simple Question) -> Set `confidence` to 1.0.
- If Request is Vague/Ambiguous -> Set `confidence` to 0.7.

**ENUMS:**
- Category: {category}
- Priority: {priority}
- Queue: {queue} (Map: BILLING->BILLING_Q, TECHNICAL->TECH_Q, ACCOUNT->ACCOUNT_Q, SHIPPING->OPS_Q, SECURITY->SECURITY_Q, PRICING->ACCOUNT_Q, OTHER->GENERAL_Q)
- Action: {next_action}

**JSON RESPONSE (Fill exactly):**
\{
  "internal_note": "Last Msg: [INSERT_TEXT]. Intent: [COMMAND/QUESTION]. Knowledge: [FOUND/EMPTY].",
  "category": "SELECT_FROM_ENUMS",
  "priority": "SELECT_FROM_ENUMS",
  "queue": "SELECT_FROM_ENUMS",
  "next_action": "SELECT_FROM_ENUMS",
  "confidence": 0.9,
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

**CRITICAL OVERRIDES (You MUST apply these):**
1. If Action=`UPDATE_CUSTOMER_PROFILE`, replace `null` in `action_parameters` with the EXTRACTED VALUE.
2. If Action=`AUTO_RESOLVE`, replace `draft_reply` `null` with a summary of `{relevant_articles}`. **DO NOT use placeholders.**
3. If Action=`ASK_CLARIFYING`, replace `clarifying_question` `null` with a specific question.
4. If Action=`ASSIGN_QUEUE`, ensure `draft_reply` is `null`.
5. **Update `confidence` based on the Calculation Rule (1.0 or 0.7).**
