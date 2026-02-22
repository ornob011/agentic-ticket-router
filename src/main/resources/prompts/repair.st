Repair invalid planner JSON. Return valid JSON only.

INVALID RESPONSE:
{failed_response}

ERROR:
{error_message}

ENUM VALUES (copy exact tokens):
- category: {category_values}
- priority: {priority_values}
- queue: {queue_values}
- next_action: {next_action_values}

REPAIR RULES:
- Fix invalid enums using the provided lists
- confidence: number in [0.0, 1.0]
- If uncertain, choose in-list token, never out-of-list
- queue uncertain → GENERAL_Q

INTENT REPAIR:
- Profile update: UPDATE_CUSTOMER_PROFILE only with concrete target value in action_parameters
- Recap: AUTO_REPLY, not USE_KNOWLEDGE_ARTICLE
- Problem with insufficient context: ASK_CLARIFYING (queue: GENERAL_Q unless clearly justified)

OUTPUT RULES:
- ASK_CLARIFYING: clarifying_question non-null, draft_reply null
- ASSIGN_QUEUE: both null
- AUTO_REPLY/AUTO_RESOLVE: draft_reply non-null (≥12 chars)
- USE_KNOWLEDGE_ARTICLE: article_id positive
- UPDATE_CUSTOMER_PROFILE: profile field value present

Return JSON only:
\{
  "category": "SELECT_FROM_ENUMS",
  "priority": "SELECT_FROM_ENUMS",
  "queue": "SELECT_FROM_ENUMS",
  "next_action": "SELECT_FROM_ENUMS",
  "confidence": 0.0,
  "clarifying_question": null,
  "draft_reply": null,
  "rationale_tags": ["tag"]
\}
