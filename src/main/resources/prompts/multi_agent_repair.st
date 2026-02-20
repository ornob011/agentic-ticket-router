Repair planner JSON and return valid JSON only.
Agent role: {agent_role}

RULES:
- Keep enum values valid.
- Preserve intent.
- Ensure nullable fields remain explicit.
- Keep rationale_tags concise and evidence-based.
- Only use these enum values:
  - category: {category}
  - priority: {priority}
  - queue: {queue}
  - next_action: {next_action}
- Never output unknown enum values.
- If enum is invalid or missing, replace it with the closest semantically valid value from the provided enum lists while preserving intent and safety.
- Output must be directly deserializable into RouterResponse.
- Follow all system-level JSON/type/format constraints.
- Ensure confidence is a JSON number in [0.0, 1.0].
- Never introduce generic placeholder draft replies.
- If next_action is AUTO_REPLY or AUTO_RESOLVE, draft_reply must be non-null and actionable.
- If next_action is ASK_CLARIFYING, clarifying_question must be non-null and draft_reply must be null.
- If next_action is ASSIGN_QUEUE, clarifying_question and draft_reply must be null.
- If next_action is USE_KNOWLEDGE_ARTICLE, action_parameters.article_id must be positive numeric.
- If next_action is USE_TEMPLATE, action_parameters.template_id must be positive numeric.
- If next_action is UPDATE_CUSTOMER_PROFILE, include at least one concrete profile field in action_parameters.
- For enum correction, copy exact token values from the provided allowed lists.
- Never alter enum token casing/underscores.
- Ensure queue/category consistency:
{routing_policy}
- Avoid escalation bias: if request is routine and safe, do not repair to HUMAN_REVIEW.

OUTPUT:
- JSON only. No markdown.

RAW_PLANNER_JSON:
{planner_raw_json}

VALIDATION_ERROR:
{validation_error}
