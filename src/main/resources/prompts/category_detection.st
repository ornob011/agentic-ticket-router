TASK: Classify the following customer support message into exactly one category.

CURRENT TICKET CATEGORY: {current_category}

CONVERSATION HISTORY:
{conversation_history}

NEW MESSAGE CONTENT:
{content}

AVAILABLE CATEGORIES:
{category}

CLASSIFICATION INSTRUCTIONS:
1. Read the NEW MESSAGE and CONVERSATION HISTORY carefully.
2. If the new message is a continuation or modification of the SAME TOPIC as the current ticket category, classify it as {current_category}.
3. Examples of SAME TOPIC (should match current category):
   - Making changes to the same field (e.g., changing company name multiple times).
   - REVERTING a recent change (e.g., changing value A to B, then back to A).
   - Correcting a mistake in the previous message.
   - Asking follow-up questions about the same issue.
4. Only classify as a DIFFERENT category if the message introduces a genuinely NEW and UNRELATED topic.
5. When in doubt between current category and a different one, prefer the current category.
6. IGNORE "System" confirmation messages when determining the user's intent. Focus on the user's flow.

RESPONSE FORMAT:
Return ONLY the category name in UPPERCASE.

STRICT RULES:
- Return ONLY the category name (one word, uppercase).
- Do NOT include "Category:" prefix or punctuation.
