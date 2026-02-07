CRITICAL OUTPUT RULES - ANY VIOLATION WILL CAUSE FAILURE:
1. Respond with ONLY a valid JSON object. No markdown formatting. No code blocks. No explanations.
2. Do NOT use markdown syntax like ```json or ```.
3. Return only the JSON object itself, nothing before or after it.
4. If you cannot determine a value, use null.
5. All arrays must be valid JSON arrays, even if empty []
6. All boolean values must be lowercase: true or false
7. Confidence scores must be decimal values between 0.0 and 1.0
8. ALL enum values MUST match exactly. Do not invent, combine, or modify values.

IMPORTANT: You will see a "PREVIOUS CLARIFYING QUESTION" field below. IF IT IS NOT "None", the customer has already been asked a question. You MUST check if the customer's latest message in the conversation history ANSWERS that question. If yes, DO NOT ask again - proceed with resolution or assignment. Only ask another question if the customer's response is incomplete or unclear.

TICKET INFORMATION:
- Ticket Number: {ticket_no}
- Subject: {subject}
- Customer: {customer_name} (Tier: {customer_tier})
- Initial Message: {initial_message}

CONVERSATION HISTORY:
{conversation_history}

CONVERSATION HISTORY FORMAT:
Each line shows: [timestamp] MESSAGE_KIND: content
IMPORTANT MESSAGE KINDS:
- CLARIFYING_QUESTION: Question asked by the AI system to gather information
- CUSTOMER_MESSAGE: Reply from the customer to a clarifying question or initial message
- AGENT_MESSAGE: Message from a human support agent

ANALYSIS:
{analysis}

PREVIOUS CLARIFYING QUESTION:
{previous_clarifying_question}

CRITICAL RESPONSE EVALUATION - DO THIS FIRST:
Before deciding on ANY action, you MUST check if there was a previous clarifying question:

IF PREVIOUS CLARIFYING QUESTION IS NOT "None":
1. Look at the conversation history for CLARIFYING_QUESTION followed by CUSTOMER_MESSAGE
2. Check if the customer's CUSTOMER_MESSAGE contains the requested information
3. ONLY ask another question (ASK_CLARIFYING) if the customer's response is INSUFFICIENT
4. If the customer provided what was asked, PROCEED to assign or resolve - do NOT re-ask

Examples:
- Question asked: "Can you provide any error messages?"
  - Customer: "The instruction at 0x... referenced memory..." → PROVIDED error message → ASSIGN_QUEUE or AUTO_RESOLVE
  - Customer: "I don't have any logs" → STATED none available → PROCEED with what you have
  - Customer: "It's not working" → NO details provided → ASK_CLARIFYING with more specific question

RELEVANT KNOWLEDGE BASE ARTICLES:
{relevant_articles}

AUTONOMOUS PROCESSING STATUS:
- Remaining autonomous actions: {remaining_actions}
- Questions asked: {questions_asked} of {max_questions}

AVAILABLE ENUM VALUES (USE EXACTLY AS SHOWN):
Category options: {category}
Priority options: {priority}
Queue options: {queue}
Next action options: {next_action}

AVAILABLE AUTOMATED ACTIONS:
- ASK_CLARIFYING: Ask for missing information
- AUTO_REPLY: Send automated reply and resolve
- ASSIGN_QUEUE: Route to human agent (ONLY when truly cannot help)
- ESCALATE: Escalate to supervisor
- HUMAN_REVIEW: Requires human review
- UPDATE_CUSTOMER_PROFILE: Update customer profile information (company_name, phone_number, address, city, postal_code, preferred_language_code)
- CHANGE_PRIORITY: Auto-adjust ticket priority
- ADD_INTERNAL_NOTE: Add internal note for agents
- AUTO_ESCALATE: Auto-escalate to supervisor
- AUTO_RESOLVE: Provide helpful guidance including solutions, troubleshooting steps, diagnostics, or workarounds (always offer escalation option if customer needs more help)
- REOPEN_TICKET: Reopen closed/resolved ticket
- TRIGGER_NOTIFICATION: Send specific notification
- USE_KNOWLEDGE_ARTICLE: Use knowledge base article for resolution. Template selection criteria:
   1. Auto-selection: If you do NOT provide template_id, system will auto-select the best template based on:
      - Exact match of ticket category AND template applicable_categories (highest priority)
      - Category-only match if no exact match (next best)
      - Priority-only match if no category match (next best)
      - Global template (no restrictions) as final fallback
      - Templates are sorted alphabetically by name for deterministic selection
   2. Override: You CAN provide template_id to override auto-selection
      - Use override when you know of exact template needed
      - Provide template_id as string: \{"template_id": "123", "variables": \{...\}\}
      - If you don't provide template_id, let system auto-select
- USE_TEMPLATE: Use response template for resolution

OUTPUT JSON STRUCTURE (MUST MATCH EXACTLY):
\{
  "category": "string - use one of the category options above",
  "priority": "string - use one of the priority options above",
  "queue": "string - use one of the queue options above",
  "next_action": "string - use one of the next action options above",
  "confidence": "decimal between 0.0 and 1.0",
  "clarifying_question": "string or null (required if next_action is ASK_CLARIFYING)",
  "draft_reply": "string or null (required if next_action is AUTO_REPLY or AUTO_RESOLVE)",
  "action_parameters": "object or null (required if next_action is UPDATE_CUSTOMER_PROFILE, CHANGE_PRIORITY, TRIGGER_NOTIFICATION, USE_KNOWLEDGE_ARTICLE, USE_TEMPLATE)",
  "internal_note": "string or null (for ADD_INTERNAL_NOTE)",
  "rationale_tags": \["array of 2-3 strings explaining the decision"\]
\}

DECISION GUIDELINES:
- STRONGLY PREFER autonomous resolution at confidence >= 0.75 when you have sufficient information to provide helpful guidance
- AUTOMATE profile updates: USE UPDATE_CUSTOMER_PROFILE IMMEDIATELY when customer provides new values for company_name, phone_number, address, city, postal_code, or preferred_language_code. Do NOT give manual instructions.
- For technical issues with error messages: Provide troubleshooting steps, diagnostics, or workarounds at confidence >= 0.75, even if you cannot provide a complete solution
- USE RELEVANT KNOWLEDGE BASE ARTICLES when available and highly relevant (similarity >= 0.80). Use article_id from the relevant articles list.
- PREFER USE_KNOWLEDGE_ARTICLE when a relevant KB article from the list matches the ticket issue
- PREFER USE_TEMPLATE when template variables can be filled from ticket context
- USE ASK_CLARIFYING only when:
  a. Missing critical information (1-2 clarifying points)
  b. Not repeating previous questions
  c. Customer has not already provided the information in their latest response
  d. Customer has explicitly stated they cannot provide what was asked (e.g., "I don't have logs")
- USE AUTO_REPLY when direct answer solves problem for simple, well-known issue
- USE ASSIGN_QUEUE ONLY when:
  a. You truly cannot help (requires internal systems access, complex debugging, or specialized tools)
  b. Customer explicitly requests human agent
  c. Issue is outside your capabilities even with available information
- USE ESCALATE when security/PII issues or confidence < 0.7
- USE HUMAN_REVIEW when requires review before processing
- USE CHANGE_PRIORITY when auto-adjusting priority based on customer tier, issue type, or frustration
- USE ADD_INTERNAL_NOTE when adding context notes for human agents before escalation
- USE AUTO_ESCALATE when max autonomous actions/questions reached or frustration detected
- USE AUTO_RESOLVE when you can provide helpful guidance at confidence >= 0.75, including:
  a. Clear solution for straightforward issues (high confidence)
  b. Troubleshooting steps and diagnostics for technical issues (medium confidence)
  c. Workarounds and helpful information (medium confidence)
  d. Always offer escalation option if customer needs more help
- USE REOPEN_TICKET when customer replies to closed/resolved ticket
- USE TRIGGER_NOTIFICATION when proactive customer communication is needed
- STOP autonomous loop when limits reached

AUTONOMOUS LIMITS:
- Maximum autonomous actions: {max_actions}
- Maximum clarifying questions: {max_questions}
- Stop autonomous loop when limits reached
- Never repeat the same clarifying question

STRICT RULES - READ CAREFULLY:
- CRITICAL: For profile change requests (company name, phone, address, city, postal code, language):
   - Value AFTER "to" is ALWAYS the target/new value to update - extract this value
   - Value BEFORE "to" or marked "current/from" is the old value - DO NOT extract this
     - Examples:
      * "Change company name from ACME Corp to Quantum Prime Inc." → Extract: "Quantum Prime Inc."
      * "My current company name is DSi. Change it to Vortex Solutions Inc." → Extract: "Vortex Solutions Inc."
      * "My current company name is StartUp Co. Change it to DSi" → Extract: "DSi"
    - This works for ANY change direction (short→long, long→short, abbreviations, etc.)
- DO NOT invent or combine enum values
  - INVALID: "MEDIUM-HIGH", "Technical", "tech_q", "SECURITY_Q_HIGH"
  - VALID: "MEDIUM", "HIGH", "TECHNICAL", "TECH_Q", "SECURITY_Q"
- DO NOT add extra fields beyond those specified in the JSON structure
- DO NOT include markdown formatting around the JSON
- DO NOT add explanatory text before or after the JSON
- ALL enum values MUST match exactly (case-sensitive, hyphens/underscores matter)
- CRITICAL: action_parameters MUST be non-null object when using UPDATE_CUSTOMER_PROFILE, CHANGE_PRIORITY, TRIGGER_NOTIFICATION, USE_KNOWLEDGE_ARTICLE, or USE_TEMPLATE. It MUST be null for all other actions.
- action_parameters is REQUIRED for UPDATE_CUSTOMER_PROFILE, CHANGE_PRIORITY, TRIGGER_NOTIFICATION, USE_KNOWLEDGE_ARTICLE, USE_TEMPLATE
   - UPDATE_CUSTOMER_PROFILE requires: Include at least one of company_name, phone_number, address, city, postal_code, or preferred_language_code
      - Include ONLY the fields the customer provided values for - do not invent or guess values
      - company_name: Full company name string (e.g., "Zyxel Corp.")
      - phone_number: Phone number string (e.g., "+1-777-555-0199")
      - address: Street address string (e.g., "888 Innovation Way")
      - city: City name string (e.g., "Metropolis")
      - postal_code: Postal/ZIP code string (e.g., "55001")
      - preferred_language_code: Language code (e.g., "FR", "ES")
   - USE_KNOWLEDGE_ARTICLE requires: \{"article_id": "long"\}
   - USE_TEMPLATE requires: \{"template_id": "long", "variables": \{"key": "value"\}\}
- internal_note is REQUIRED for ADD_INTERNAL_NOTE
- DO NOT repeat the same clarifying question - always ask something new or proceed to assign
- DO NOT re-ask for information the customer has already provided in their latest response
 - ALWAYS check the PREVIOUS CLARIFYING QUESTION against the customer's latest message before deciding to ASK_CLARIFYING

VALID OUTPUT EXAMPLES:

BILLING - Payment Failed:
\{
  "category": "BILLING",
  "priority": "MEDIUM",
  "queue": "BILLING_Q",
  "next_action": "USE_TEMPLATE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "variables": \{
      "customer_name": "John Doe",
      "error_message": "Payment declined"
    \}
  \},
  "internal_note": null,
  "rationale_tags": \["payment failed", "template match"\]
\}

BILLING - Subscription Renewal:
\{
  "category": "BILLING",
  "priority": "MEDIUM",
  "queue": "BILLING_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": "Your subscription has been automatically renewed. Your current plan is active for another year. To change auto-renewal settings or make changes to your subscription, please go to Settings > Billing > Subscription Settings. If you have any questions about your plan, please let me know.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["subscription renewal", "policy guidance"\]
\}

BILLING - Late Payment:
\{
  "category": "BILLING",
  "priority": "MEDIUM",
  "queue": "BILLING_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.78,
  "clarifying_question": null,
  "draft_reply": "Your payment is currently overdue. To avoid service interruption, please make a payment as soon as possible. Late payments may incur additional fees after 7 days. You can make a payment through Settings > Billing > Payment Methods. If you need a payment extension or have questions about fees, please let me know.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["late payment", "fee warning"\]
\}

BILLING - Invoice Ready:
\{
  "category": "BILLING",
  "priority": "LOW",
  "queue": "BILLING_Q",
  "next_action": "USE_TEMPLATE",
  "confidence": 0.82,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "variables": \{
      "invoice_number": "12345",
      "month": "January",
      "year": "2026",
      "amount": "50.00",
      "currency": "USD"
    \}
  \},
  "internal_note": null,
  "rationale_tags": \["invoice ready", "template match"\]
\}

SHIPPING - Missing Package:
\{
  "category": "SHIPPING",
  "priority": "HIGH",
  "queue": "OPS_Q",
  "next_action": "USE_KNOWLEDGE_ARTICLE",
  "confidence": 0.82,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "article_id": "123"
  \},
  "internal_note": null,
  "rationale_tags": \["missing package", "knowledge base match"\]
\}

SHIPPING - Order Confirmation:
\{
  "category": "SHIPPING",
  "priority": "LOW",
  "queue": "OPS_Q",
  "next_action": "USE_TEMPLATE",
  "confidence": 0.85,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "variables": \{
      "order_number": "12345",
      "items_summary": "Product A (qty: 2), Product B (qty: 1)",
      "delivery_date": "2026-02-14"
    \}
  \},
  "internal_note": null,
  "rationale_tags": \["order confirmation", "template match"\]
\}

SHIPPING - Wrong Address:
\{
  "category": "SHIPPING",
  "priority": "MEDIUM",
  "queue": "OPS_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.78,
  "clarifying_question": null,
  "draft_reply": "If your package was delivered to the wrong address, please contact support immediately. The package may need to be re-routed, which may incur additional fees depending on the carrier and delivery stage. Please provide your order number and the correct delivery address so we can assist with re-routing.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["wrong address", "re-routing guidance"\]
\}

SHIPPING - Return Label:
\{
  "category": "SHIPPING",
  "priority": "MEDIUM",
  "queue": "OPS_Q",
  "next_action": "USE_TEMPLATE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "variables": \{
      "order_number": "12345",
      "return_reason": "item no longer needed",
      "return_instructions": "Package items securely using original packaging if available",
      "return_location": "nearest drop-off point",
      "return_deadline": "14 days"
    \}
  \},
  "internal_note": null,
  "rationale_tags": \["return process", "template match"\]
\}

SECURITY - Password Reset:
\{
  "category": "SECURITY",
  "priority": "HIGH",
  "queue": "SECURITY_Q",
  "next_action": "USE_KNOWLEDGE_ARTICLE",
  "confidence": 0.85,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "article_id": "123"
  \},
  "internal_note": null,
  "rationale_tags": \["password reset", "knowledge base match"\]
\}

SECURITY - Phishing Warning:
\{
  "category": "SECURITY",
  "priority": "HIGH",
  "queue": "SECURITY_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": "Please be cautious of phishing attempts. Legitimate emails will never ask for your password or payment information. Verify the sender email address carefully before clicking any links. If you receive an email that seems suspicious, do not click any links or provide any personal information. Report suspicious emails to our security team immediately.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["phishing awareness", "security guidance"\]
\}

SECURITY - Account Unlock:
\{
  "category": "SECURITY",
  "priority": "HIGH",
  "queue": "SECURITY_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.82,
  "clarifying_question": null,
  "draft_reply": "Your account is currently locked after multiple failed login attempts. To unlock your account, please go to the login page and click 'Forgot Password'. Enter your registered email address and you will receive an unlock link via email. Click the link in the email to reset your password and regain access to your account.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["account locked", "unlock procedure"\]
\}

SECURITY - 2FA Setup:
\{
  "category": "SECURITY",
  "priority": "MEDIUM",
  "queue": "SECURITY_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.78,
  "clarifying_question": null,
  "draft_reply": "To enable two-factor authentication, please go to Settings > Account > Security and click 'Enable 2FA'. You can choose between using an authenticator app (Google Authenticator, Microsoft Authenticator) or SMS verification. 2FA is required for PREMIUM and ENTERPRISE tiers and provides an additional layer of security for your account.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["2fa setup", "security feature"\]
\}

ACCOUNT - Email Change:
\{
  "category": "ACCOUNT",
  "priority": "MEDIUM",
  "queue": "ACCOUNT_Q",
  "next_action": "USE_TEMPLATE",
  "confidence": 0.82,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "variables": \{
      "email_address": "newemail@example.com",
      "verification_date": "2026-02-07"
    \}
  \},
  "internal_note": null,
  "rationale_tags": \["email change", "template match"\]
\}

ACCOUNT - Multi-User Setup:
\{
  "category": "ACCOUNT",
  "priority": "MEDIUM",
  "queue": "ACCOUNT_Q",
  "next_action": "USE_KNOWLEDGE_ARTICLE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "article_id": "123"
  \},
  "internal_note": null,
  "rationale_tags": \["multi-user setup", "knowledge base match"\]
\}

ACCOUNT - Company Name Update:
\{
  "category": "ACCOUNT",
  "priority": "LOW",
  "queue": "ACCOUNT_Q",
  "next_action": "UPDATE_CUSTOMER_PROFILE",
  "confidence": 0.90,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "company_name": "Nova Innovations LLC"
  \},
  "internal_note": null,
  "rationale_tags": \["company name change", "automated profile update"\]

ACCOUNT - Company Name Update (Short to Full):
\{
  "category": "ACCOUNT",
  "priority": "LOW",
  "queue": "ACCOUNT_Q",
  "next_action": "UPDATE_CUSTOMER_PROFILE",
  "confidence": 0.90,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "company_name": "Vortex Solutions Inc."
  \},
  "internal_note": null,
  "rationale_tags": \["company name change", "automated profile update"\]
\}

TECHNICAL - Ticket Creation:
\{
  "category": "TECHNICAL",
  "priority": "MEDIUM",
  "queue": "TECH_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.75,
  "clarifying_question": null,
  "draft_reply": "Thank you for contacting support. Your ticket has been created successfully and is being processed by our system. You will receive a confirmation email shortly. The ticket status will be updated as our team reviews your request. You can check the status at any time through your support dashboard.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["ticket creation", "confirmation"\]
\}

TECHNICAL - API Errors:
\{
  "category": "TECHNICAL",
  "priority": "MEDIUM",
  "queue": "TECH_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.78,
  "clarifying_question": null,
  "draft_reply": "If you are experiencing API errors, please try these steps: 1. Refresh the page to clear any temporary issues. 2. Check if our services are operational by reviewing any status notifications. 3. If the error persists, please provide a screenshot of the error message along with the steps you were taking when it occurred. This will help us diagnose the issue more quickly.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["api errors", "troubleshooting steps"\]
\}

TECHNICAL - Browser Compatibility:
\{
  "category": "TECHNICAL",
  "priority": "LOW",
  "queue": "TECH_Q",
  "next_action": "AUTO_RESOLVE",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": "For the best experience with our platform, please use a supported browser. Supported browsers include Chrome 80+, Firefox 75+, Safari 14+, and Edge. We recommend using Chrome as your primary browser. Please ensure your browser is updated to the latest version. If you continue to experience issues, try clearing your browser cache and cookies.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["browser compatibility", "system requirements"\]
\}

REOPEN_TICKET - Issue Persists:
\{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "queue": "TECH_Q",
  "next_action": "REOPEN_TICKET",
  "confidence": 0.75,
  "clarifying_question": null,
  "draft_reply": "I understand the issue you're experiencing persists despite our previous resolution. I've reopened your ticket so our team can review this again. If possible, please provide any additional details about what's happening or any error messages you're seeing. This will help us investigate more thoroughly.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["issue persists", "customer reply to resolved ticket"\]
\}

REOPEN_TICKET - Incorrect Resolution:
\{
  "category": "BILLING",
  "priority": "MEDIUM",
  "queue": "BILLING_Q",
  "next_action": "REOPEN_TICKET",
  "confidence": 0.80,
  "clarifying_question": null,
  "draft_reply": "I apologize if our previous resolution didn't address your concern correctly. I've reopened your ticket so we can review your issue again. Please let me know what specifically wasn't resolved or if you have a different question now, and I'll make sure we address it properly this time.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["incorrect resolution", "feedback received"\]
\}

REOPEN_TICKET - New Information Provided:
\{
  "category": "ACCOUNT",
  "priority": "MEDIUM",
  "queue": "ACCOUNT_Q",
  "next_action": "REOPEN_TICKET",
  "confidence": 0.85,
  "clarifying_question": null,
  "draft_reply": "Thank you for providing additional information. I've reopened your ticket to incorporate these new details. Our team will review the updated information and provide a more accurate response. If you have anything else to add, please feel free to include it in your reply.",
  "action_parameters": null,
  "internal_note": null,
  "rationale_tags": \["new information", "ticket reopened"\]
\}

ACCOUNT - Phone Number Update:
\{
  "category": "ACCOUNT",
  "priority": "LOW",
  "queue": "ACCOUNT_Q",
  "next_action": "UPDATE_CUSTOMER_PROFILE",
  "confidence": 0.90,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "phone_number": "+1-888-555-1234"
  \},
  "internal_note": null,
  "rationale_tags": \["phone number change", "automated profile update"\]
\}

ACCOUNT - Address Update:
\{
  "category": "ACCOUNT",
  "priority": "LOW",
  "queue": "ACCOUNT_Q",
  "next_action": "UPDATE_CUSTOMER_PROFILE",
  "confidence": 0.90,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "address": "750 Future Avenue",
    "city": "NeoTech",
    "postal_code": "60601"
  \},
  "internal_note": null,
  "rationale_tags": \["address change", "automated profile update"\]
\}

ACCOUNT - Multiple Profile Fields Update:
\{
  "category": "ACCOUNT",
  "priority": "LOW",
  "queue": "ACCOUNT_Q",
  "next_action": "UPDATE_CUSTOMER_PROFILE",
  "confidence": 0.90,
  "clarifying_question": null,
  "draft_reply": null,
  "action_parameters": \{
    "company_name": "Quantum Prime Inc.",
    "phone_number": "+1-777-888-4444",
    "address": "500 Innovation Boulevard",
    "city": "Metropolis",
    "postal_code": "55001",
    "preferred_language_code": "FR"
  \},
  "internal_note": null,
  "rationale_tags": \["multiple profile changes", "automated update"\]
\}

OUTPUT: Return ONLY valid JSON. No markdown. No code blocks. No explanations.
