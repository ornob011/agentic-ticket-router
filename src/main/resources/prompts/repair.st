PREVIOUS INVALID RESPONSE:
{failed_response}

ERROR MESSAGE:
{error_message}

AVAILABLE ENUM VALUES (COPY EXACT TOKENS ONLY):
- category: {category_values}
- priority: {priority_values}
- queue: {queue_values}
- next_action: {next_action_values}

REPAIR RULES (STRICT):
- Return JSON only.
- Preserve original user intent while fixing invalid structure/enums.
- Do not output reasoning labels/classes as enum values.
- category/priority/queue/next_action must be exact tokens from available enum lists.
- confidence must be numeric in [0.0, 1.0].

ENUM CORRECTION (MANDATORY):
- If ERROR MESSAGE mentions $.category, overwrite category with one exact token from category list.
- If ERROR MESSAGE mentions $.priority, overwrite priority with one exact token from priority list.
- If ERROR MESSAGE mentions $.queue, overwrite queue with one exact token from queue list.
- If ERROR MESSAGE mentions $.next_action, overwrite next_action with one exact token from next_action list.
- Never keep an invalid enum from PREVIOUS INVALID RESPONSE.
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

ACTION CONTRACTS:
- AUTO_REPLY/AUTO_RESOLVE => draft_reply non-null
- ASK_CLARIFYING => clarifying_question non-null and draft_reply null
- ASSIGN_QUEUE => clarifying_question null and draft_reply null
- USE_KNOWLEDGE_ARTICLE => action_parameters.article_id positive
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

OUTPUT JSON STRUCTURE (MUST MATCH EXACTLY):
\{
  "category": "copy one exact category option",
  "priority": "copy one exact priority option",
  "queue": "copy one exact queue option",
  "next_action": "copy one exact next action option",
  "confidence": 0.0,
  "clarifying_question": null,
  "draft_reply": null,
  "rationale_tags": ["array of strings"]
\}
