# Agentic System Test Prompts

Test prompts for validating all 14 agentic actions in the ticket routing system.

## Quick Reference

| Action                    | Trigger Condition                              | Subject                 | Message Body                                  |
|---------------------------|------------------------------------------------|-------------------------|-----------------------------------------------|
| `UPDATE_CUSTOMER_PROFILE` | Profile update command (verb + field + value)  | Update my contact info  | Change my phone number to +1-555-123-4567     |
| `USE_KNOWLEDGE_ARTICLE`   | Question matching KB article                   | Password reset help     | How do I reset my password?                   |
| `ASK_CLARIFYING`          | Vague/ambiguous request                        | Need help               | I need help with my account                   |
| `ASSIGN_QUEUE`            | Question with no KB match                      | Question about policies | What is your company's sustainability policy? |
| `ESCALATE`                | Security category or dangerous tags            | Security concern        | I think my account was hacked                 |
| `HUMAN_REVIEW`            | Low confidence or CRITICAL with low confidence | Complex issue           | Multi-part ambiguous request                  |
| `AUTO_REPLY`              | Simple resolvable with draft reply             | Thank you               | Thanks, that resolved my issue!               |
| `AUTO_RESOLVE`            | Complete solution available                    | Return policy question  | What is your return policy?                   |
| `CHANGE_PRIORITY`         | Urgency language detected                      | URGENT: System down     | My production system is completely down!      |
| `ADD_INTERNAL_NOTE`       | Context needing agent visibility               | Ongoing issue history   | 3 months of issues, 5 agents contacted        |
| `TRIGGER_NOTIFICATION`    | Status update requested                        | Shipping notification   | Please notify me when shipped                 |
| `REOPEN_TICKET`           | Customer reply on resolved ticket              | Issue not resolved      | The issue is still happening                  |
| `AUTO_ESCALATE`           | Max autonomous actions reached                 | Need assistance         | Multiple clarification rounds                 |
| `USE_TEMPLATE`            | Template-based response match                  | Hello                   | Hi there                                      |

---

## Detailed Test Prompts

### 1. UPDATE_CUSTOMER_PROFILE

**Trigger:** Profile update command detected (verb + profile field + explicit value)

**Profile Fields:** company_name, phone_number, address, city, postal_code, preferred_language_code

**Update Verbs:** change, update, set, modify, replace, correct

| Subject                | Message Body                                                       | Expected Field Updated     |
|------------------------|--------------------------------------------------------------------|----------------------------|
| Phone number change    | Please change my phone number to +1-555-123-4567                   | phone_number               |
| Company name update    | Update my company name to Acme Corporation                         | company_name               |
| Address correction     | Set my address to 123 Main Street, Suite 100                       | address                    |
| City update request    | I want to modify my city to San Francisco                          | city                       |
| Postal code correction | Please correct my postal code to 94102                             | postal_code                |
| Language preference    | Change my preferred language to Spanish                            | preferred_language_code    |
| Contact info update    | Update phone to 555-999-8888 and company to TechCorp               | phone_number, company_name |
| Full address change    | My new address is 456 Oak Ave, Portland, OR 97201                  | address, city, postal_code |
| Billing address update | Please update my billing address to 789 Pine St, Seattle, WA 98101 | address, city, postal_code |
| Mobile number change   | I need to set my mobile phone to (415) 555-0199                    | phone_number               |

---

### 2. USE_KNOWLEDGE_ARTICLE

**Trigger:** Customer question matches a knowledge base article

**Precondition:** Knowledge base must contain relevant articles

| Subject                   | Message Body                        | Expected KB Topic       |
|---------------------------|-------------------------------------|-------------------------|
| Password reset            | How do I reset my password?         | Password reset          |
| Business hours inquiry    | What are your business hours?       | Business hours          |
| Order tracking            | How can I track my order?           | Order tracking          |
| Return policy             | What is your return policy?         | Returns/refunds         |
| Subscription cancellation | How do I cancel my subscription?    | Subscription management |
| Refund status             | Where is my refund?                 | Refund status           |
| Payment method update     | How do I update my payment method?  | Payment methods         |
| Payment options           | What payment methods do you accept? | Accepted payments       |
| Contact support           | How do I contact customer support?  | Contact info            |
| Shipping information      | What is your shipping policy?       | Shipping info           |

---

### 3. ASK_CLARIFYING

**Trigger:** Vague or ambiguous request that cannot be categorized

| Subject              | Message Body                     | Why It Triggers Clarification |
|----------------------|----------------------------------|-------------------------------|
| Help needed          | Help                             | Too vague, no context         |
| Issue                | I have an issue                  | No specific problem stated    |
| Account problem      | Account problem                  | No details about the problem  |
| Something wrong      | Something is wrong               | No actionable information     |
| Assistance required  | I need assistance                | No specific request           |
| Help me              | Can you help me?                 | No context provided           |
| Not working          | It's not working                 | No indication of what "it" is |
| Question             | I have a question                | No question asked             |
| Problem              | Problem with my stuff            | Too vague                     |
| Issue with something | Issue with the thing             | No specific item or problem   |
| General inquiry      | I need to know something         | No specific information       |
| Account issue        | There's an issue with my account | No details provided           |

---

### 4. ASSIGN_QUEUE

**Trigger:** Clear question/request but no matching KB article

**Outcome:** Routes to appropriate queue (BILLING_Q, TECH_Q, OPS_Q, ACCOUNT_Q, GENERAL_Q)

| Subject                  | Message Body                                      | Expected Queue |
|--------------------------|---------------------------------------------------|----------------|
| Sustainability policy    | What is your company's sustainability policy?     | GENERAL_Q      |
| Internship opportunities | I want to know about your internship program      | GENERAL_Q      |
| Data center locations    | Can you tell me about your data center locations? | GENERAL_Q      |
| API rate limits          | I need information about your API rate limits     | TECH_Q         |
| Tax calculation error    | My invoice shows an incorrect tax amount          | BILLING_Q      |
| Subscription plan change | I want to change my subscription plan             | ACCOUNT_Q      |
| Wrong delivery address   | My package was delivered to the wrong address     | OPS_Q          |
| Account tier upgrade     | How do I upgrade my account tier?                 | ACCOUNT_Q      |
| Duplicate charge         | I was charged twice for the same order            | BILLING_Q      |
| Slow website             | The website is loading very slowly                | TECH_Q         |
| Feature request          | I would like to request a new feature             | GENERAL_Q      |
| Partnership inquiry      | How can I become a partner?                       | GENERAL_Q      |
| API documentation        | Where can I find your API documentation?          | TECH_Q         |
| Invoice discrepancy      | My invoice amount doesn't match my order          | BILLING_Q      |

---

### 5. ESCALATE

**Trigger:** Security category OR dangerous rationale tags

**Dangerous Tags:** THREAT, PII_RISK, SECURITY_BREACH, HACK, BREACH, VULNERABILITY, MALWARE, CYBER_ATTACK, EXPLOIT

**Policy Override:** Always escalates to SECURITY_Q with HIGH priority

| Subject                | Message Body                                          | Trigger Reason          |
|------------------------|-------------------------------------------------------|-------------------------|
| Account hacked         | I think my account was hacked                         | HACK, SECURITY category |
| Security breach report | There has been a security breach on my account        | SECURITY_BREACH         |
| Vulnerability found    | I found a vulnerability in your system                | VULNERABILITY           |
| Threat report          | Someone is threatening me through your platform       | THREAT                  |
| Suspicious email       | I received a suspicious email claiming to be from you | CYBER_ATTACK, MALWARE   |
| Data exposure          | My personal information was exposed                   | PII_RISK                |
| Payment exploit        | There's an exploit in your payment page               | EXPLOIT                 |
| System compromised     | I think your system has been compromised              | SECURITY_BREACH         |
| Malware received       | I received malware from your website                  | MALWARE                 |
| Account breach attempt | Someone is trying to breach my account                | BREACH                  |
| Unauthorized access    | I see login attempts from locations I don't recognize | SECURITY category       |
| Phishing attempt       | Someone is phishing using your company name           | CYBER_ATTACK            |
| Data leak concern      | I'm concerned about a potential data leak             | PII_RISK                |
| Security vulnerability | I discovered a security flaw in your application      | VULNERABILITY           |

---

### 6. HUMAN_REVIEW

**Trigger:** Low confidence (< auto-route threshold) OR CRITICAL priority with low confidence

**Policy:** Requires human intervention before processing

| Subject                        | Message Body                                                                                                                                                                             | Why It Needs Human Review      |
|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| Complex multi-department issue | I need help with something really complicated involving my account, billing, and a legal matter that I can't explain right now but it's very important and involves multiple departments | Too complex, ambiguous         |
| CRITICAL undefined issue       | CRITICAL: Something terrible is happening but I can't tell you what it is exactly                                                                                                        | CRITICAL + vague               |
| Unclear routing needed         | I have a situation that requires immediate attention but I'm not sure if this is the right place and I might need to talk to someone else first                                          | Low confidence, unclear intent |
| Cross-functional problem       | There's an issue that spans across billing, technical support, and account management with possible legal implications                                                                   | Multi-domain complexity        |
| Supervisor request             | I need to discuss something confidential that I can only share with a supervisor                                                                                                         | Explicit supervisor request    |
| Legal matter                   | I have a legal concern that requires discussion with management                                                                                                                          | Legal complexity               |
| Regulatory compliance question | I need information about your compliance with specific regulations                                                                                                                       | Regulatory complexity          |
| Executive escalation request   | I would like to speak with someone in executive management                                                                                                                               | Executive involvement needed   |

---

### 7. AUTO_REPLY

**Trigger:** LLM generates draft reply and sets action to AUTO_REPLY

**Outcome:** Sends reply and resolves ticket

| Subject                | Message Body                                                        | Context                    |
|------------------------|---------------------------------------------------------------------|----------------------------|
| Thank you              | Thank you so much for your help, that resolved my issue completely! | Gratitude/closure          |
| Issue resolved         | That worked perfectly, I don't need any further assistance.         | Confirmation of resolution |
| Close ticket           | Yes, the solution you provided fixed it. You can close this ticket. | Explicit close request     |
| Self-resolved          | I figured it out on my own, thanks anyway!                          | Self-resolved              |
| All good               | Perfect, that's exactly what I needed. Goodbye!                     | Satisfaction + closure     |
| Thanks for the help    | Thanks, everything is working now!                                  | Resolution confirmed       |
| Problem solved         | The issue is fixed now, thank you for your assistance.              | Problem solved             |
| No further help needed | I don't need any more help, you can close this.                     | Closure request            |

---

### 8. AUTO_RESOLVE

**Trigger:** Complete solution can be provided autonomously

**Difference from AUTO_REPLY:** Typically used for initial ticket resolution with solution content

| Subject                | Message Body                         | Resolution Type         |
|------------------------|--------------------------------------|-------------------------|
| Return policy question | What is your return policy?          | FAQ answer + resolve    |
| Unsubscribe request    | How do I unsubscribe from emails?    | Instruction + resolve   |
| Contact information    | What is your phone number?           | Contact info + resolve  |
| Location inquiry       | Where are you located?               | Location info + resolve |
| Shipping cost          | How much does shipping cost?         | Pricing info + resolve  |
| International shipping | Do you offer international shipping? | Policy info + resolve   |
| Fax number request     | What is your fax number?             | Contact info + resolve  |
| Order number lookup    | How do I find my order number?       | Instruction + resolve   |
| Store hours            | What are your store hours?           | Hours info + resolve    |
| Email address          | What is your support email?          | Contact info + resolve  |

---

### 9. CHANGE_PRIORITY

**Trigger:** Urgency indicators or explicit priority change request

**Outcome:** Updates ticket priority (CRITICAL, HIGH, MEDIUM, LOW)

| Subject                 | Message Body                                         | Expected Priority |
|-------------------------|------------------------------------------------------|-------------------|
| URGENT: Production down | URGENT: My production system is completely down!     | CRITICAL          |
| Critical revenue loss   | This is critical - I'm losing money every minute!    | CRITICAL          |
| Emergency assistance    | I need this resolved immediately, it's an emergency! | CRITICAL or HIGH  |
| Multiple users affected | This is high priority, affecting multiple users      | HIGH              |
| Deadline approaching    | Please treat this as urgent, I have a deadline       | HIGH              |
| Need help soon          | I need help soon but it's not an emergency           | MEDIUM            |
| Low priority inquiry    | Whenever you get a chance, low priority              | LOW               |
| Quick question          | Not urgent, just a quick question                    | LOW               |
| System outage           | CRITICAL: Our entire system is offline!              | CRITICAL          |
| Time-sensitive request  | This is time-sensitive, please prioritize            | HIGH              |

---

### 10. ADD_INTERNAL_NOTE

**Trigger:** Context that requires documentation for agents but not visible to customer

**Note:** Typically combined with other actions

| Subject                     | Message Body                                                                     | Internal Note Content        |
|-----------------------------|----------------------------------------------------------------------------------|------------------------------|
| Long-standing issue         | I've been having this issue for 3 months and spoke to 5 different agents already | Document interaction history |
| Related ticket reference    | This is related to ticket #12345 that was closed last week                       | Cross-reference note         |
| Frequent complaints         | Customer mentioned this is their 4th complaint this month                        | Customer behavior note       |
| Legal escalation warning    | They said they might escalate to their lawyer if not resolved                    | Risk escalation note         |
| Accessibility accommodation | Customer has a disability and needs accessible communication                     | Accessibility accommodation  |
| VIP customer                | I've been a premium customer for 10 years with significant spend                 | VIP status note              |
| Previous agent conflict     | The last agent I spoke with was unhelpful                                        | Agent quality note           |
| Escalation history          | I've already escalated this twice before                                         | Escalation history           |

---

### 11. TRIGGER_NOTIFICATION

**Trigger:** Explicit notification request or status change requiring customer awareness

**Parameters:** notification_type, title, body

| Subject                       | Message Body                                      | Notification Type |
|-------------------------------|---------------------------------------------------|-------------------|
| Shipping notification request | Please notify me when my order ships              | STATUS_CHANGE     |
| Ticket update request         | Send me an update when someone looks at my ticket | STATUS_CHANGE     |
| Refund status notification    | Let me know when my refund is processed           | STATUS_CHANGE     |
| Confirmation email request    | Can you send me a confirmation email?             | STATUS_CHANGE     |
| Change notifications          | I want to be notified about any changes           | STATUS_CHANGE     |
| Delivery notification         | Please email me when my package is delivered      | STATUS_CHANGE     |
| Status update request         | Keep me updated on the progress of this ticket    | STATUS_CHANGE     |
| Resolution notification       | Let me know when this is resolved                 | STATUS_CHANGE     |

---

### 12. REOPEN_TICKET

**Trigger:** Customer responds to a resolved/closed ticket indicating issue persists

**Precondition:** Ticket must be in RESOLVED or CLOSED status

**Note:** This is typically triggered by customer reply, not LLM decision

| Subject (Reply)       | Message Body (Reply)                          | Context                  |
|-----------------------|-----------------------------------------------|--------------------------|
| Re: Issue resolved    | The issue is still happening                  | Problem not resolved     |
| Re: Solution provided | That didn't fix it                            | Solution didn't work     |
| Re: Ticket closed     | I'm still experiencing the problem            | Ongoing issue            |
| Re: Resolved          | It worked for a day but now it's broken again | Recurrence               |
| Re: Fixed             | Actually, I'm still having trouble            | False resolution         |
| Re: Complete          | The fix didn't work for me                    | Unsuccessful resolution  |
| Re: Done              | The problem came back                         | Regression               |
| Re: Closed            | I'm not satisfied with the resolution         | Customer dissatisfaction |

---

### 13. AUTO_ESCALATE

**Trigger:** System-triggered when max autonomous actions reached (default: 5)

**Precondition:** Multiple rounds of clarification without resolution

**Note:** Requires autonomous limits to be hit, not direct LLM decision

**Test Sequence for AUTO_ESCALATE:**

| Step | Subject             | Message Body       |
|------|---------------------|--------------------|
| 1    | Need assistance     | I need help        |
| 2    | Re: Clarification   | With something     |
| 3    | Re: More details    | It's not working   |
| 4    | Re: Additional info | I already told you |
| 5    | Re: Please specify  | This is ridiculous |

→ Should trigger AUTO_ESCALATE after max actions

**Alternative Test Scenarios:**

| Scenario                 | Sequence of Replies                                                                                                          |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------|
| Circular conversation    | "Help" → "With account" → "It's broken" → "The thing" → "I don't know"                                                       |
| Frustration detection    | "Help" → "Still need help" → "This is taking too long" → "I've explained this 5 times already!" → "Why isn't this resolved?" |
| Vague repeated responses | "Issue" → "Problem" → "Not working" → "Still broken" → "Help me"                                                             |

---

### 14. USE_TEMPLATE

**Trigger:** Match for template-based response

**Parameters:** template_id

**Precondition:** Templates must exist in the system

| Subject               | Message Body                              | Template Type                     |
|-----------------------|-------------------------------------------|-----------------------------------|
| Hello                 | Hello                                     | Greeting template                 |
| Hi                    | Hi there                                  | Greeting template                 |
| Complaint             | I want to file a complaint                | Complaint acknowledgment template |
| First contact         | This is my first time contacting support  | New customer welcome template     |
| Long-time customer    | I'm a long-time customer                  | Loyal customer acknowledgment     |
| Ticket acknowledgment | Can you acknowledge receipt of my ticket? | Acknowledgment template           |
| General greeting      | Good morning                              | Greeting template                 |
| Introduction          | Hi, I'm new here                          | New customer welcome template     |

---

## Test Scenarios by Category

### Billing (BILLING → BILLING_Q)

| Subject                | Message Body                                | Expected Action                       |
|------------------------|---------------------------------------------|---------------------------------------|
| Double charge          | I was charged twice for my order            | ASSIGN_QUEUE or USE_KNOWLEDGE_ARTICLE |
| Invoice discrepancy    | Why is my invoice amount different?         | ASSIGN_QUEUE or ASK_CLARIFYING        |
| Refund request         | I want a refund for my purchase             | USE_KNOWLEDGE_ARTICLE or ASSIGN_QUEUE |
| Billing address update | Update my billing address to 123 Main St    | UPDATE_CUSTOMER_PROFILE               |
| Payment failed         | My payment keeps failing                    | ASSIGN_QUEUE                          |
| Tax question           | Can you explain the tax charges on my bill? | ASSIGN_QUEUE                          |

### Technical (TECHNICAL → TECH_Q)

| Subject             | Message Body                             | Expected Action                       |
|---------------------|------------------------------------------|---------------------------------------|
| App crash           | The app keeps crashing on startup        | ASSIGN_QUEUE                          |
| Login issue         | I can't log into my account              | USE_KNOWLEDGE_ARTICLE or ASSIGN_QUEUE |
| Server error        | Error 500 when submitting form           | ASSIGN_QUEUE                          |
| Page not loading    | The page won't load                      | ASK_CLARIFYING or ASSIGN_QUEUE        |
| Feature not working | The export feature is not working        | ASSIGN_QUEUE                          |
| Integration issue   | Your API is returning unexpected results | ASSIGN_QUEUE                          |

### Account (ACCOUNT → ACCOUNT_Q)

| Subject           | Message Body                              | Expected Action                       |
|-------------------|-------------------------------------------|---------------------------------------|
| Email change      | I want to change my email address         | UPDATE_CUSTOMER_PROFILE               |
| Account deletion  | How do I delete my account?               | USE_KNOWLEDGE_ARTICLE                 |
| Username recovery | I forgot my username                      | USE_KNOWLEDGE_ARTICLE or ASSIGN_QUEUE |
| Settings issue    | My account settings won't save            | ASSIGN_QUEUE                          |
| Account lockout   | My account is locked                      | ASSIGN_QUEUE                          |
| Profile update    | Please update my company name to ABC Corp | UPDATE_CUSTOMER_PROFILE               |

### Shipping/Ops (SHIPPING → OPS_Q)

| Subject          | Message Body                             | Expected Action                         |
|------------------|------------------------------------------|-----------------------------------------|
| Package tracking | Where is my package?                     | USE_KNOWLEDGE_ARTICLE or ASSIGN_QUEUE   |
| Address change   | I need to change my shipping address     | UPDATE_CUSTOMER_PROFILE or ASSIGN_QUEUE |
| Damaged delivery | My order was damaged in shipping         | ASSIGN_QUEUE                            |
| Expedite request | Can I expedite my delivery?              | USE_KNOWLEDGE_ARTICLE or ASSIGN_QUEUE   |
| Missing items    | Some items were missing from my delivery | ASSIGN_QUEUE                            |
| Delivery delay   | My package is late                       | ASSIGN_QUEUE                            |

### Security (SECURITY → SECURITY_Q → ESCALATE)

| Subject             | Message Body                                      | Expected Action |
|---------------------|---------------------------------------------------|-----------------|
| Unauthorized access | I think someone accessed my account               | ESCALATE        |
| Suspicious activity | There's suspicious activity on my account         | ESCALATE        |
| Phishing report     | I received a phishing email                       | ESCALATE        |
| Password compromise | My password was compromised                       | ESCALATE        |
| Account takeover    | Someone changed my password without my permission | ESCALATE        |
| Data breach concern | I think my data was part of a breach              | ESCALATE        |

---

## Testing Instructions

### Prerequisites

1. Ensure knowledge base has seed articles for USE_KNOWLEDGE_ARTICLE tests
2. Ensure templates exist for USE_TEMPLATE tests
3. Create a resolved ticket for REOPEN_TICKET tests

### Test Workflow

1. Log in as customer
2. Create new ticket with test subject and message body
3. Observe the action taken by the agentic system
4. Check ticket status, queue assignment, and any generated messages
5. For multi-step tests (AUTO_ESCALATE), continue the conversation

### Verification Checklist

- [ ] Correct action executed
- [ ] Correct category assigned
- [ ] Correct priority assigned
- [ ] Correct queue assigned
- [ ] Appropriate message generated (if applicable)
- [ ] Audit trail created
- [ ] Notification sent (if applicable)
