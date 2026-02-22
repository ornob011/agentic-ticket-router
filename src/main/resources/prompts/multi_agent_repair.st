Repair planner JSON and return valid JSON only.
Agent role: {agent_role}

AVAILABLE ENUM VALUES (COPY EXACT TOKENS ONLY):
- category: {category}
- priority: {priority}
- queue: {queue}
- next_action: {next_action}

REPAIR RULES (STRICT):
- Return JSON only.
- Preserve original user intent while fixing invalid structure/enums.
- Do not output reasoning labels/classes as enum values.
- category/priority/queue/next_action must be exact tokens from available enum lists.
- confidence must be numeric in [0.0, 1.0].

ENUM CORRECTION (MANDATORY):
- If VALIDATION_ERROR mentions $.category, overwrite category with one exact token from category list.
- If VALIDATION_ERROR mentions $.priority, overwrite priority with one exact token from priority list.
- If VALIDATION_ERROR mentions $.queue, overwrite queue with one exact token from queue list.
- If VALIDATION_ERROR mentions $.next_action, overwrite next_action with one exact token from next_action list.
- Never keep an invalid enum from RAW_PLANNER_JSON.
- If uncertain, select a safe in-list token; never emit out-of-list text.
- Never synthesize queue tokens; queue must be copied from queue list.
- If queue cannot be confidently mapped, set queue to GENERAL_Q.

INTENT-SPECIFIC REPAIR:
- For explicit profile update intent:
  - next_action = UPDATE_CUSTOMER_PROFILE
  - keep UPDATE_CUSTOMER_PROFILE only when a concrete target profile value is present in action_parameters
  - include at least one non-empty profile field value in action_parameters
  - keep values aligned with latest customer request representation in the failed response
  - do not introduce profile field values that are absent from the failed response payload
- For recap-about-prior-action intent:
  - next_action = AUTO_REPLY
  - do not set USE_KNOWLEDGE_ARTICLE
  - draft_reply must be non-null and based on prior assistant/system action context

REPAIR INTENT GATE (MANDATORY):
- Keep or set next_action to UPDATE_CUSTOMER_PROFILE only when failed-response context shows all three in latest customer turn:
  - mutation verb signal,
  - profile field signal,
  - concrete target value signal.
- If latest customer turn asks for explanation/recap and lacks concrete new target value, force next_action to AUTO_REPLY.
- Do not preserve no-change/update outcomes for recap_or_explain_intent turns.
- CONTEXT-COLLECTION REPAIR GATE:
  - If latest customer turn reports a problem and actionable context is insufficient, force:
    - next_action = ASK_CLARIFYING
    - queue = GENERAL_Q unless a more specific queue is clearly justified
    - priority = MEDIUM unless explicit high-risk signals are present, then HIGH
  - Treat context as insufficient when fewer than 3 of the following are known:
    - execution environment/context,
    - concrete observed behavior,
    - timing/reproducibility,
    - recent change/trigger,
    - concrete evidence (error output, logs, or reproducible steps).
  - Do not keep AUTO_REPLY or USE_KNOWLEDGE_ARTICLE when this gate applies.

ACTION CONTRACTS:
- AUTO_REPLY/AUTO_RESOLVE => draft_reply non-null and non-empty
- ASK_CLARIFYING => clarifying_question non-null and draft_reply null
- Context-gated ASK_CLARIFYING => clarifying_question must be a numbered 3-5 question checklist focused on missing details and include one risk-check line when risk is plausible.
- ASSIGN_QUEUE => clarifying_question null and draft_reply null
- USE_KNOWLEDGE_ARTICLE => action_parameters.article_id positive
- USE_TEMPLATE => action_parameters.template_id positive
- UPDATE_CUSTOMER_PROFILE => at least one concrete profile field value

DISAMBIGUATION REPAIR RULE:
- If latest customer turn in failed response context is a new explicit update command with a concrete target value, set next_action to UPDATE_CUSTOMER_PROFILE.
- Do not keep AUTO_REPLY for a new explicit profile update command.
- If latest customer turn is interrogative/explanatory and has no concrete new profile field value, set next_action to AUTO_REPLY.
- UPDATE_CUSTOMER_PROFILE is forbidden when repaired action_parameters lacks concrete profile field values from latest customer turn.
- If repaired response has next_action = UPDATE_CUSTOMER_PROFILE but no concrete target profile field value, repair away from UPDATE_CUSTOMER_PROFILE.
- If repaired response is question-only/recap-only without concrete target profile values, use AUTO_REPLY.

FINAL ENUM SELF-CHECK (MUST PASS):
- category is exactly one token from category list.
- priority is exactly one token from priority list.
- queue is exactly one token from queue list.
- next_action is exactly one token from next_action list.

FINAL ACTION-FIELD SELF-CHECK (MUST PASS):
- If next_action is AUTO_REPLY or AUTO_RESOLVE:
  - draft_reply must be a JSON string (not null), trimmed length >= 12.
  - draft_reply must not be placeholder/null-like text.
  - clarifying_question must be null.
- If next_action is ASK_CLARIFYING:
  - clarifying_question must be a non-empty string, trimmed length >= 8.
  - draft_reply must be null.
  - for context-gated tickets, clarifying_question must contain 3-5 numbered questions and one risk-check when risk is plausible.
- If next_action is ASSIGN_QUEUE:
  - clarifying_question must be null.
  - draft_reply must be null.
- If next_action is USE_KNOWLEDGE_ARTICLE:
  - action_parameters.article_id must be a positive integer.
- If next_action is USE_TEMPLATE:
  - action_parameters.template_id must be a positive integer.
- If next_action is UPDATE_CUSTOMER_PROFILE:
  - at least one profile field in action_parameters must be non-null and non-empty.
  - draft_reply must be null.

ROUTING CONSISTENCY:
{routing_policy}

RAW_PLANNER_JSON:
{planner_raw_json}

VALIDATION_ERROR:
{validation_error}
