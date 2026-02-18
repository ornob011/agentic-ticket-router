Repair planner JSON and return valid JSON only.

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

OUTPUT:
- JSON only. No markdown.

RAW_PLANNER_JSON:
{planner_raw_json}

VALIDATION_ERROR:
{validation_error}
