Repair invalid planner JSON. Return valid JSON only.
Agent role: {agent_role}

ENUM VALUES (copy exact tokens):
- category: {category}
- priority: {priority}
- queue: {queue}
- next_action: {next_action}

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
- USE_TEMPLATE: template_id positive
- UPDATE_CUSTOMER_PROFILE: profile field value present

ROUTING CONSISTENCY:
{routing_policy}

RAW_PLANNER_JSON:
{planner_raw_json}

VALIDATION_ERROR:
{validation_error}

Return JSON only:
\{
  "internal_note": "short reasoning summary",
  "category": "SELECT_FROM_ENUMS",
  "priority": "SELECT_FROM_ENUMS",
  "queue": "SELECT_FROM_ENUMS",
  "next_action": "SELECT_FROM_ENUMS",
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
  "rationale_tags": ["tag"]
\}
