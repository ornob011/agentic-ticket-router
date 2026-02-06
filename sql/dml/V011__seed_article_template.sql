-- =====================================================
-- Article Templates Seeding (200+ templates)
-- =====================================================

BEGIN;

-- =====================================================
-- INSERT ALL TEMPLATES INTO article_template TABLE
-- =====================================================

INSERT INTO article_template (name, template_content, variables, active)
VALUES
-- PASSWORD & ACCOUNT TEMPLATES (10)
('Password Reset Template',
 'Hi {{customer_name}}, Your password has been reset. Temporary password: {{temp_password}}. Please change it immediately after logging in.',
 '["customer_name", "temp_password"]',
 true),

('Account Unlock Notification',
 'Your account has been unlocked. Please log in with your credentials. If you did not request this unlock, please contact support immediately.',
 '[]',
 true),

('2FA Setup Guide',
 'Two-factor authentication is now enabled. Please download an authenticator app (Google Authenticator, Microsoft Authenticator) and scan the QR code. This additional security measure is required for your account tier.',
 '[]',
 true),

('Profile Update Confirmation',
 'Your profile has been updated. Changes may take up to 24 hours to reflect across the platform.',
 '[]',
 true),

('Email Change Verification',
 'Please verify your new email address by clicking the link in the confirmation email. Your old email address will become inactive after verification.',
 '[]',
 true),

('Account Delete Warning',
 'Your account deletion request has been submitted. Please note: This action cannot be undone and your data will be permanently deleted after 30 days.',
 '[]',
 true),

('Password Reset Success',
 'We have successfully reset your password. Your new password is: {{new_password}}. Please log in and change it immediately for security purposes.',
 '["new_password"]',
 true),

('Tier Upgrade Confirmation',
 'Your subscription has been upgraded to {{new_tier}}. You now have access to premium features. Your next billing date will reflect the new tier pricing.',
 '["new_tier"]',
 true),

('Address Update Success',
 'Your shipping/billing address has been updated successfully. Future orders and invoices will use this address.',
 '[]',
 true),

('Tax ID Added',
 'Your tax ID has been added to your account. Please upload verification documents if requested. This may take 1-2 business days to verify.',
 '[]',
 true),

-- BILLING & PAYMENT TEMPLATES (17)
('Payment Successful',
 'Payment of {{amount}} {{currency}} has been successfully processed. A receipt has been sent to {{customer_name}}. Transaction ID: {{transaction_id}}.',
 '["customer_name", "amount", "currency", "transaction_id"]',
 true),

('Payment Failed',
 'Your payment could not be processed. Error: {{error_message}}. Please try a different payment method or contact support. Your payment method has not been charged.',
 '["customer_name", "error_message"]',
 true),

('Invoice Ready',
 'Invoice #{{invoice_number}} for {{month}} {{year}} is now available. Amount: {{amount}} {{currency}}. Due: {{due_date}}. Auto-payment is enabled.',
 '["invoice_number", "month", "year", "amount", "currency", "due_date"]',
 true),

('Subscription Renewed',
 'Your subscription has been automatically renewed. Your plan: {{plan_name}} has been extended for another year. Next billing date: {{next_billing_date}}. Receipt available in Settings > Billing.',
 '["plan_name", "next_billing_date"]',
 true),

('Refund Initiated',
 'Your refund request has been initiated. Amount: {{refund_amount}}. Estimated processing time: 5-7 business days. We will notify you once the refund is processed. Refund ID: {{refund_id}}.',
 '["refund_amount", "refund_id"]',
 true),

('Refund Approved',
 'Your refund has been approved. The amount of {{refund_amount}} has been credited to your account balance. Transaction ID: {{refund_id}}. This refund will appear on your next invoice.',
 '["refund_amount", "refund_id"]',
 true),

('Payment Method Updated',
 'Your payment method has been updated successfully. Your new payment method will be used for future transactions. If you have auto-pay enabled, your next payment will be processed using the new method on {{next_billing_date}}.',
 '[]',
 true),

('Credit Applied',
 'Credit balance of {{credit_amount}} has been applied to your order #{{order_number}}. Remaining balance: {{remaining_balance}}.',
 '["credit_amount", "order_number", "remaining_balance"]',
 true),

('Payment Overdue',
 'Your payment of {{amount}} {{currency}} is overdue by {{days_overdue}} days. To avoid service interruption, please make a payment immediately. Late fees may apply if payment is not received within 7 days of the due date.',
 '["amount", "currency", "days_overdue"]',
 true),

('Auto-Payment Failed',
 'Your auto-payment could not be processed. Please ensure your payment method is valid and has sufficient funds. You can: 1. Update payment method 2. Add alternative payment method 3. Contact support if issue persists',
 '[]',
 true),

('Multi-Invoice Combined',
 'Multiple invoices have been combined into a single payment. Invoices: {{invoice_list}}. Total amount: {{total_amount}}. You can view details for each invoice in Settings > Billing > Invoices.',
 '["invoice_list", "total_amount"]',
 true),

('Gift Card/Redeemption',
 'Gift card or credit redemption successful. Amount: {{amount}}. Your gift card/credit balance has been updated. Redemption code: {{redemption_code}}. Transaction ID: {{transaction_id}}.',
 '["amount", "redemption_code", "transaction_id"]',
 true),

('Split Payment Success',
 'Your order #{{order_number}} has been successfully split into {{num_payments}} payments. Each payment amount: {{payment_amount}}. Total paid: {{total_amount}}.',
 '["order_number", "num_payments", "payment_amount", "total_amount"]',
 true),

('Late Payment Fee Notification',
 'A late payment fee of {{fee_amount}} has been charged to your account due to payment being {{days_late}} days past the due date. The fee is a percentage of your monthly payment based on your subscription tier. Please ensure future payments are made on time to avoid additional fees.',
 '["fee_amount", "days_late"]',
 true),

('Subscription Downgraded',
 'Your subscription has been downgraded from {{old_tier}} to {{new_tier}}. This change will take effect on {{effective_date}}. Some premium features are no longer available. You can upgrade anytime to restore full access.',
 '["old_tier", "new_tier", "effective_date"]',
 true),

('Currency Changed',
 'Your currency preference has been updated from {{old_currency}} to {{new_currency}}. All future transactions and invoices will be displayed in your selected currency. You can change your currency preference anytime in Settings > Billing.',
 '["old_currency", "new_currency"]',
 true),

('Discount Applied',
 'Discount code {{discount_code}} has been successfully applied to your order #{{order_number}}. Original price: {{original_price}}. Discounted price: {{discounted_price}}. You saved: {{savings_amount}}. This discount cannot be combined with other offers.',
 '["discount_code", "order_number", "original_price", "discounted_price", "savings_amount"]',
 true),

-- ORDER & SHIPPING TEMPLATES (17)
('Order Confirmation',
 'Thank you for your order #{{order_number}}! We have received your order successfully. Order details: {{items_summary}}. Estimated delivery: {{delivery_date}}. You will receive a confirmation email shortly with your order information and tracking details.',
 '["order_number", "items_summary", "delivery_date"]',
 true),

('Order Shipped',
 'Great news! Your order #{{order_number}} has been shipped. Carrier: {{carrier}}. Tracking number: {{tracking_number}}. Estimated delivery: {{estimated_delivery}}. Track your package in real-time: {{tracking_url}}. Thank you for your business!',
 '["order_number", "carrier", "tracking_number", "estimated_delivery", "tracking_url"]',
 true),

('Order Delivered',
 'Your order #{{order_number}} has been delivered. Delivery address: {{delivery_address}}. Delivered on: {{delivery_date}}. Please leave a review and let us know if everything was satisfactory. Your feedback helps us improve our service!',
 '["order_number", "delivery_address", "delivery_date"]',
 true),

('Order Delayed',
 'Your order #{{order_number}} is experiencing a delay. We apologize for the inconvenience. New estimated delivery: {{new_delivery_date}}. We are working to expedite your shipment. You can track the latest status at: {{tracking_url}}.',
 '["order_number", "new_delivery_date", "tracking_url"]',
 true),

('Out for Delivery',
 'Your package is out for delivery today by {{carrier}}. Carrier: {{carrier}}. Tracking: {{tracking_number}}. Delivery address: {{delivery_address}}. Please ensure someone is available to receive the package.',
 '["order_number", "carrier", "tracking_number", "delivery_address"]',
 true),

('Pickup Ready',
 'Your order #{{order_number}} is ready for pickup. Store: {{store_name}}. Pickup address: {{pickup_address}}. Available for pickup: {{pickup_window}}. Please bring your order number and a valid ID to collect your package. Store hours: {{store_hours}}.',
 '["order_number", "store_name", "pickup_address", "pickup_window", "store_hours"]',
 true),

('Shipping Method Changed',
 'Your shipping method has been updated to: {{new_shipping_method}}. Your preference has been saved and will apply to future orders.',
 '["new_shipping_method"]',
 true),

('Return Label Generated',
 'Your return shipping label has been generated. Order #{{order_number}}. Reason: {{return_reason}}. Instructions: {{return_instructions}}. Package your items securely using appropriate packaging. Drop off at: {{return_location}}. Return deadline: {{return_deadline}}. Print and attach the label to your package. Return shipping is prepaid.',
 '["order_number", "return_reason", "return_instructions", "return_location", "return_deadline"]',
 true),

('Partial Shipment',
 'Your order #{{order_number}} is being shipped in multiple shipments. Shipment {{shipment_number}} contains: {{items}}. Remaining shipments will follow shortly. Each shipment has its own tracking number.',
 '["order_number", "shipment_number", "items"]',
 true),

('International Shipping',
 'Your international order #{{order_number}} has been processed. Customs information: {{customs_info}}. Estimated delivery: {{delivery_date}}. Please review import duties and taxes that may apply. Tracking: {{tracking_number}}.',
 '["order_number", "customs_info", "delivery_date", "tracking_number"]',
 true),

('Address Update',
 'Your shipping address has been updated. Order #{{order_number}}. New address: {{new_address}}. Please ensure this is correct before confirming. If order has already shipped, it may not be possible to change the address.',
 '["order_number", "new_address"]',
 true),

('Shipping Rate Applied',
 'Your shipping rate has been applied to order #{{order_number}}. Standard rate: {{standard_rate}}. Express rate: {{express_rate}}. Free shipping threshold: {{free_threshold}}.',
 '["order_number", "standard_rate", "express_rate", "free_threshold"]',
 true),

('Carrier Update',
 'Your order has been assigned to a new carrier: {{carrier_name}}. Tracking number: {{tracking_number}}. The carrier will handle delivery and provide status updates.',
 '["order_number", "carrier_name", "tracking_number"]',
 true),

('Delivery Window',
 'Your scheduled delivery window is {{delivery_window}}. Carrier: {{carrier_name}}. The driver will attempt delivery between {{start_time}} and {{end_time}}. Please ensure someone is available during this window.',
 '["carrier_name", "start_time", "end_time"]',
 true),

('Residential Delivery',
 'Your order is scheduled for residential delivery. Apartment: {{apartment_number}}. Doorman: {{doorman_available}}. Special instructions: {{special_instructions}}. Please provide access code if required: {{access_code}}.',
 '["apartment_number", "doorman_available", "special_instructions", "access_code"]',
 true),

('Business Delivery',
 'Your order is scheduled for business delivery. Company: {{company_name}}. Business hours: {{business_hours}}. Dock #: {{dock_number}}. Receiving contact: {{contact_name}}. Phone: {{contact_phone}}. Please ensure your loading dock is available during the delivery window.',
 '["company_name", "business_hours", "dock_number", "contact_name", "contact_phone"]',
 true),

('Saturday Delivery',
 'Your order is scheduled for Saturday delivery. Delivery date: {{delivery_date}}. Carrier: {{carrier}}. Please ensure someone is available to receive the package on Saturday.',
 '["delivery_date", "carrier"]',
 true),

-- TECHNICAL & ACCOUNT TEMPLATES (17)
('Ticket Created',
 'Your support ticket #{{ticket_number}} has been created successfully. You will receive a confirmation email at {{customer_email}} shortly. Please add any relevant details or attachments to help us resolve your issue faster.',
 '["ticket_number", "customer_email"]',
 true),

('Ticket Updated',
 'Your support ticket #{{ticket_number}} has been updated. Updated on: {{updated_date}}. Changes: {{changes_summary}}. Your assigned agent: {{agent_name}} will be notified. Please keep your ticket number for reference.',
 '["ticket_number", "updated_date", "changes_summary", "agent_name"]',
 true),

('Ticket Assigned',
 'Your ticket #{{ticket_number}} has been assigned to {{agent_name}} in the {{department}} department. Response time: {{response_time}}. Estimated resolution: {{estimated_resolution}}. Please check your dashboard for updates and respond to any clarifying questions.',
 '["ticket_number", "agent_name", "department", "response_time", "estimated_resolution"]',
 true),

('Ticket Escalated',
 'Your ticket #{{ticket_number}} has been escalated to the escalation team. Reason: {{escalation_reason}}. Supervisor: {{supervisor_name}}. You can expect a response within {{response_time}}. If your issue is urgent, please contact us directly via our priority support line.',
 '["ticket_number", "escalation_reason", "supervisor_name", "response_time"]',
 true),

('Ticket Resolved',
 'Your support ticket #{{ticket_number}} has been resolved. Resolution summary: {{resolution_summary}}. Resolved on: {{resolved_date}} by {{agent_name}}. Please take a moment to rate our service. Your feedback helps us improve. If you need further assistance, please reply to this ticket.',
 '["ticket_number", "resolution_summary", "resolved_date", "agent_name"]',
 true),

('Ticket Closed',
 'Your support ticket #{{ticket_number}} has been closed. Closure reason: {{closure_reason}}. If you disagree with this closure, please reopen the ticket within 7 days. Thank you for using our support system!',
 '["ticket_number", "closure_reason"]',
 true),

('2FA Disabled',
 'Two-factor authentication has been temporarily disabled for your account due to: {{reason}}. For security purposes, you may be required to contact support to re-enable 2FA. Your account remains secure with your password only. If you did not request this change, please contact us immediately.',
 '["reason"]',
 true),

('API Key Generated',
 'A new API key has been generated for your account. Key: {{api_key_prefix}}***{{masked_key}}. Please save this key in a secure location. This key provides {{permissions}} permissions. Never share your API key with anyone. Key expires: {{expiration_date}}.',
 '["api_key_prefix", "masked_key", "permissions", "expiration_date"]',
 true),

('API Key Rotated',
 'Your API key has been rotated. Old key deactivated: {{deactivation_date}}. New key generated: {{generation_date}}. Please update any applications using the old key with the new key. Key ID: {{key_id}}.',
 '["deactivation_date", "generation_date", "key_id"]',
 true),

('Device Added',
 'A new device has been added to your account. Device: {{device_name}}. Device type: {{device_type}}. Last active: {{last_active}}. If you did not add this device, please contact support immediately. Manage your devices in Settings > Account > Security > Trusted Devices.',
 '["device_name", "device_type", "last_active"]',
 true),

('Session Terminated',
 'Your session has been terminated due to: {{reason}}. All unsaved changes have been lost. You have been logged out from all devices. If this was unexpected, please contact support and verify your account activity.',
 '["reason"]',
 true),

('Password Changed',
 'Your password has been changed successfully. Changed on: {{change_date}}. Location: {{location}}. Device: {{device_used}}. For security purposes, you will be logged out from all other devices. If you did not make this change, please contact support immediately.',
 '["change_date", "location", "device_used"]',
 true),

('Email Verified',
 'Your email address {{email_address}} has been successfully verified. Your account is now fully active. You can now access all features of your account. Verification completed on: {{verification_date}}.',
 '["email_address", "verification_date"]',
 true),

('Profile Updated',
 'Your profile has been updated. Updated fields: {{updated_fields}}. Changes will be reflected across the platform within 24 hours.',
 '["updated_fields"]',
 true),

('Company Profile Created',
 'Your company profile has been created successfully. Company: {{company_name}}. Industry: {{industry}}. Employee count: {{employee_count}}. Billing contacts: {{billing_contacts}} configured. Please complete your company profile by adding team members and setting up billing preferences.',
 '["company_name", "industry", "employee_count", "billing_contacts"]',
 true),

('Notification Preferences',
 'Your notification preferences have been updated. Email notifications: {{email_enabled}}. SMS notifications: {{sms_enabled}}. In-app notifications: {{in_app_enabled}}. Browser notifications: {{browser_enabled}}. Changes will take effect immediately.',
 '["email_enabled", "sms_enabled", "in_app_enabled", "browser_enabled"]',
 true),

('Account Merge',
 'Your account merge request has been processed. From account: {{source_account}}. To account: {{target_account}}. Data consolidation complete. All data from the source account has been transferred to the target account. The source account will be closed. You can now access all features from your merged account.',
 '["source_account", "target_account"]',
 true),

-- SECURITY TEMPLATES (10)
('Security Alert',
 'We detected a security event on your account: Event Type: {{event_type}}. Date: {{event_date}}. Location: {{location}}. Device: {{device}}. Description: {{description}}. If you recognize this activity, no action is required. If this is suspicious, please contact support immediately by replying to this email. Your account security is our top priority.',
 '["event_type", "event_date", "location", "device", "description"]',
 true),

('Password Compromised',
 'Your password may have been compromised. We have temporarily locked your account for your protection. Please change your password immediately. To unlock: 1. Click "Forgot Password" on the login page 2. Enter your email address 3. Follow the reset instructions 4. Your account will be unlocked after password change 5. Enable 2FA for additional security 6. Review recent account activity for any unauthorized access 7. Contact security@domain.com if you did not initiate this request',
 '[]',
 true),

('Phishing Warning',
 'We have detected a potential phishing attempt targeting your account. Attempted login from: {{suspicious_ip}} at {{attempt_time}}. If you did not attempt this login, your account remains secure. Security tips: Never click on links in unsolicited emails. Verify sender addresses carefully. Report phishing to security@domain.com.',
 '["suspicious_ip", "attempt_time"]',
 true),

('New Device Login',
 'A new login was detected from a device you haven''t used before. Device: {{device_name}}. Location: {{location}}. Time: {{login_time}}. If this was you, no action is needed. If this wasn''t you, please secure your account and change your password immediately. Review your trusted devices in Settings > Account > Security > Trusted Devices.',
 '["device_name", "location", "login_time"]',
 true),

('2FA Added',
 'Two-factor authentication has been enabled on your account. Authentication method: {{auth_method}}. Added on: {{added_date}}. For enhanced security, please download and configure your authenticator app. Backup codes: {{backup_codes}} provided. Your account is now more secure with 2FA enabled.',
 '["auth_method", "added_date", "backup_codes"]',
 true),

('Suspicious Activity Blocked',
 'We detected suspicious activity on your account and blocked it to protect your security. Activity: {{activity_description}}. Time: {{activity_time}}. IP Address: {{ip_address}}. Your account is secure. If you believe this was a mistake, please contact support to verify your identity.',
 '["activity_description", "activity_time", "ip_address"]',
 true),

('API Key Expiring',
 'Your API key will expire on {{expiration_date}}. Key ID: {{key_id}}. Please rotate or regenerate your key before expiration to avoid service interruption. Current usage: {{usage_count}} API calls in the last 30 days.',
 '["expiration_date", "key_id", "usage_count"]',
 true),

('Data Export Complete',
 'Your data export request has been completed. Export type: {{export_type}}. Files generated: {{file_list}}. Data includes: {{data_included}}. Download link: {{download_link}}. The download link will be available for 7 days. Your data will be permanently deleted from our servers after 30 days. Contact support if you need an extension.',
 '["export_type", "file_list", "data_included", "download_link"]',
 true),

('Session Management Warning',
 'We detected {{session_count}} active sessions across multiple devices. This may indicate unauthorized access. Please review your active sessions in Settings > Account > Security > Session Management and revoke any sessions you don''t recognize. For your security, we recommend logging out from all devices when not in use.',
 '["session_count"]',
 true),

('Security Check',
 'Our security team has completed a routine security check on your account. Status: All systems secure. No vulnerabilities found. Recommendations: {{recommendations}}. Next scheduled check: {{next_check_date}}. Your account security is important to us. Please continue following best practices: use strong passwords, enable 2FA, and report suspicious activity.',
 '["recommendations", "next_check_date"]',
 true),

-- GENERAL & SUPPORT TEMPLATES (15)
('Welcome Message',
 'Welcome to {{platform_name}}! We''re excited to have you as a {{user_type}}. Getting started is easy with our step-by-step guide. Your dashboard is your central hub where you can access all features, track your tickets, and manage your settings. Take a moment to explore the platform and let us know if you need any assistance!',
 '["platform_name", "user_type"]',
 true),

('Ticket Response',
 'Thank you for your patience. Your support ticket #{{ticket_number}} has been updated with new information. Update type: {{update_type}}. Added by: {{added_by}} at {{added_date}}. Please check your dashboard for the latest status. We''re committed to providing you with the best possible support experience.',
 '["ticket_number", "update_type", "added_by", "added_date"]',
 true),

('Resolution Follow-up',
 'We hope your issue has been resolved to your satisfaction. Your ticket #{{ticket_number}} has been closed. Resolution: {{resolution}}. If you have any further questions or if the issue persists, please don''t hesitate to create a new ticket referencing {{original_ticket_number}}. We value your feedback and continuously work to improve our services. Thank you for choosing {{platform_name}}!',
 '["ticket_number", "resolution", "original_ticket_number", "platform_name"]',
 true),

('Feature Announcement',
 'We''re excited to announce a new feature: {{feature_name}}. {{feature_description}}. This feature is available to all {{user_type}} users starting from {{availability_date}}. Benefits: {{benefits}}. We believe this enhancement will significantly improve your experience with our platform. Check our Help Center for detailed tutorials and guides. We look forward to your feedback!',
 '["feature_name", "feature_description", "benefits", "user_type", "availability_date"]',
 true),

('Maintenance Notification',
 'Scheduled Maintenance: We will be performing scheduled maintenance on {{maintenance_date}} from {{start_time}} to {{end_time}}. Duration: {{duration}}. Expected impact: {{impact}}. During this time, some features may be temporarily unavailable. We apologize for any inconvenience and appreciate your patience. Your data will remain secure. Thank you for your understanding!',
 '["maintenance_date", "start_time", "end_time", "duration", "impact"]',
 true),

('System Status',
 'Current Platform Status: All systems operational. Uptime: {{uptime_percentage}}%. Response time: {{avg_response_time}}. Active tickets: {{active_tickets}}. Resolved today: {{resolved_today}}. We''re committed to maintaining a reliable and efficient platform. If you experience any issues, please report them through our support system. Thank you for using {{platform_name}}!',
 '["uptime_percentage", "avg_response_time", "active_tickets", "resolved_today", "platform_name"]',
 true),

('Privacy Policy Update',
 'Our Privacy Policy has been updated. Effective date: {{effective_date}}. Key changes: {{key_changes}}. Please review the updated policy in your account settings. We are committed to protecting your personal information and providing transparent control over your data. Thank you for your continued trust in {{platform_name}}.',
 '["effective_date", "key_changes"]',
 true),

('Terms Update',
 'Our Terms of Service have been updated. Effective date: {{effective_date}}. Acceptance: Continued use of our platform indicates acceptance of the updated terms. Please review the updated terms in your account settings. If you have any questions, please contact our support team. We appreciate your business and look forward to serving you!',
 '["effective_date"]',
 true),

('Account Verification',
 'Your email address {{email_address}} requires verification. A verification link has been sent to your email. Please click the link to complete the verification process. This step is required to ensure the security of your account and verify your identity. The link will expire in 24 hours. If you did not request this verification, please ignore this email.',
 '["email_address"]',
 true),

('Feedback Request',
 'Thank you for your feedback! Your feedback has been submitted for ticket #{{ticket_number}}. We carefully review all feedback and use it to improve our services. Your input helps us create a better experience for everyone. If you provided your email address, we may contact you for a follow-up survey. We appreciate you taking the time to share your thoughts with us!',
 '["ticket_number"]',
 true),

('Community Guidelines',
 'Welcome to our community! We value respectful and constructive discussions. Please review our Community Guidelines before participating: 1. Be respectful to all community members 2. Stay on topic and keep discussions relevant 3. No spam or self-promotion 4. Be helpful and supportive 5. Protect privacy - don''t share personal information 6. Report issues to moderators 7. Follow all platform rules and terms of service. We want everyone to have a positive experience in our community!',
 '[]',
 true),

('Help Article Suggestion',
 'Based on your recent activity, we thought these articles might be helpful: {{article_list}}. Knowledge base is continuously updated with new content. Browse by category or search by keywords. Still need help? Create a ticket and our team will be happy to assist you!',
 '["article_list"]',
 true),

('Feature Tutorial',
 'Feature Tutorial: {{feature_name}}. Step-by-step guide: {{tutorial_steps}}. Video tutorial available: {{has_video}}. Estimated time: {{estimated_time}}. Tips: {{tips}}. Difficulty: {{difficulty}}. Need help? Contact support through ticket, email, or live chat. Our team is here to help you make the most of {{platform_name}} features!',
 '["feature_name", "tutorial_steps", "has_video", "estimated_time", "tips", "difficulty"]',
 true),

('Quick Actions Reference',
 'Quick Actions Reference: Navigate quickly to common tasks. Create ticket: Click "New Ticket" button in dashboard. View tickets: See all your open and closed tickets. Update profile: Go to Settings > Profile. Change password: Settings > Security. Check orders: Go to Orders page. Track order: Find your package with tracking number. Contact support: Multiple channels available - email, phone, live chat. For urgent issues, use priority support channels.',
 '[]',
 true),

-- ENRICHMENT - Additional Templates (18)
('Account Verification Required',
 'Your account requires verification before you can access certain features. Please complete the verification process by: 1. Checking your email for the verification link 2. Clicking the link to confirm your identity 3. Following any additional prompts (phone verification if required). This verification helps us maintain account security. If you did not receive the verification email, please check your spam folder or request a new verification code.',
 '[]',
 true),

('Profile Incomplete Warning',
 'Your profile is incomplete. To ensure the best experience, please complete your profile by adding: 1. Your full name 2. A valid phone number 3. Your company name (if applicable) 4. Complete address information. This information helps us provide personalized support. Navigate to Settings > Profile to complete your profile.',
 '[]',
 true),

('Security Enhancement Required',
 'To enhance your account security, we recommend enabling the following: 1. Two-factor authentication (2FA) - provides an extra layer of protection 2. Login notifications - receive alerts for new sign-ins 3. Strong password - use a unique, complex password for your account 4. Trusted devices - manage and review devices with access. You can configure these in Settings > Security. These measures help protect your account from unauthorized access.',
 '[]',
 true),

('Payment Plan Update',
 'Your payment plan has been updated. Plan: {{plan_name}}. Monthly amount: {{monthly_amount}} {{currency}}. Next billing date: {{next_billing_date}}. Changes will take effect on your next billing cycle. View your updated billing details in Settings > Billing. If you have questions, please reply to this ticket.',
 '["plan_name", "monthly_amount", "currency", "next_billing_date"]',
 true),

('Payment Retry Successful',
 'Your payment retry was successful! Amount: {{amount}} {{currency}}. Transaction ID: {{transaction_id}}. Your account is now current. Thank you for your patience. If you were experiencing service interruptions, these should now be resolved. View your updated account status and billing history in Settings > Billing.',
 '["amount", "currency", "transaction_id"]',
 true),

('Prorated Credit Applied',
 'A prorated credit has been applied to your account. Credit amount: {{credit_amount}} {{currency}}. Reason: Plan change or mid-cycle upgrade. This credit will be automatically applied to your next invoice. Your current balance: {{current_balance}} {{currency}}. View your billing history for full details of this transaction.',
 '["credit_amount", "currency", "current_balance"]',
 true),

('Shipping Label Download',
 'Your shipping label is ready for download. Order #{{order_number}}. Download link: {{label_url}}. Instructions: 1. Print the label on A4 or letter paper 2. Attach firmly to package 3. Remove any old shipping labels 4. Drop off at designated location 5. Keep drop-off receipt for tracking. Label expires on: {{expiration_date}}. Please ship before the expiration date to avoid issues.',
 '["order_number", "label_url", "expiration_date"]',
 true),

('Delivery Confirmation Request',
 'We are confirming delivery details for order #{{order_number}}. Please confirm: 1. Delivery address: {{delivery_address}} 2. Delivery date: {{delivery_date}} 3. Delivery timeframe: {{timeframe}} 4. Special instructions: {{instructions}}. Reply to this ticket with your confirmation. If these details are incorrect, please let us know immediately so we can adjust the delivery.',
 '["order_number", "delivery_address", "delivery_date", "timeframe", "instructions"]',
 true),

('Package Pickup Notification',
 'Your package is ready for pickup. Order #{{order_number}}. Pickup location: {{pickup_location}}. Pickup window: {{pickup_time}}. Please bring: 1. Your order number 2. A valid ID (driver''s license or government ID) 3. The pickup confirmation email (show on your phone). Pickup will be held for {{hold_duration}} days. If you cannot pick up within this timeframe, please contact us to reschedule.',
 '["order_number", "pickup_location", "pickup_time", "hold_duration"]',
 true),

('Password Expiring Warning',
 'Your password will expire in {{days_until_expiry}} days. For your account security, we recommend changing your password before it expires. To change your password: 1. Go to Settings > Security > Password 2. Enter your current password 3. Create a new strong password (minimum 8 characters, mix of letters, numbers, symbols) 4. Confirm the new password. If you did not initiate this request, please contact support immediately.',
 '["days_until_expiry"]',
 true),

('Session Timeout Warning',
 'Your session has been inactive for {{inactive_minutes}} minutes and has been timed out for security purposes. This is a security feature to protect your account when you step away from your computer. Please log in again to continue. Your work was not saved. If you experience frequent session timeouts, you can adjust your session timeout settings in Settings > Security.',
 '["inactive_minutes"]',
 true),

('Maintenance Completed',
 'Scheduled maintenance has been completed. The platform is now fully operational. Thank you for your patience during the maintenance window. During maintenance: {{maintenance_summary}}. All features are now available. If you experience any issues accessing the platform, please clear your browser cache or try logging out and back in. We appreciate your understanding as we work to improve our services.',
 '["maintenance_summary"]',
 true),

('Unusual Activity Alert',
 'We detected unusual activity on your account that doesn''t match your normal usage patterns. Activity details: {{activity_type}} on {{activity_date}} from {{location}}. If this was you, no action is needed. If this wasn''t you, please: 1. Change your password immediately 2. Review your recent login history 3. Enable 2FA if not already enabled 4. Report any suspicious activity to our security team. Your account security is our top priority.',
 '["activity_type", "activity_date", "location"]',
 true),

('Suspicious Login Blocked',
 'We blocked a suspicious login attempt to your account. Details: {{attempt_time}} from IP address: {{ip_address}}. This login did not match your security settings or appeared to be from an unusual location. For your protection, this login was blocked. If this was you attempting to log in, please: 1. Wait 15 minutes and try again 2. Verify your email and password are correct 3. Consider enabling 2FA for additional security. If you did not attempt this login, please secure your account by changing your password.',
 '["attempt_time", "ip_address"]',
 true),

('Device Trust Confirmation',
 'A new device was added to your account. Device: {{device_name}} - {{device_type}}. If you added this device, no further action is needed. If you did NOT add this device, please take immediate action: 1. Go to Settings > Security > Trusted Devices 2. Select this device and click "Remove" 3. Change your password 4. Review your account activity for any unauthorized access. Removing unauthorized devices helps protect your account and personal information.',
 '["device_name", "device_type"]',
 true),

('System Update Notification',
 'A new system update has been deployed to the platform. Update version: {{version}}. Release date: {{release_date}}. New features and improvements: {{improvements_summary}}. Performance enhancements: {{performance_notes}}. Bug fixes: {{bug_fixes_summary}}. No action required from your end. These updates are deployed automatically. We hope you enjoy the improved experience! If you encounter any issues after this update, please create a ticket and let us know.',
 '["version", "release_date", "improvements_summary", "performance_notes", "bug_fixes_summary"]',
 true),

('Feature Feedback Request',
 'Thank you for your feedback on {{feature_name}}! Your feedback has been recorded and will be reviewed by our product team. Feedback type: {{feedback_type}}. Rating: {{rating}}/5. Comments: "{{user_comments}}". We value your input as it helps us prioritize future improvements. If you provided your email, you may be contacted for follow-up questions. Stay tuned for updates on features and improvements in our release notes.',
 '["feature_name", "feedback_type", "rating", "user_comments"]',
 true),

-- GLOBAL TEMPLATES (No Category/Priority Restrictions) (7)
('Welcome Message',
 'Hi {{customer_name}}, Welcome to our platform! We''re excited to have you on board. Your account has been successfully created. Getting started is easy: 1. Complete your profile with your personal and company information 2. Explore our comprehensive knowledge base for self-service 3. Create a support ticket if you need any assistance 4. Check out our features and tools designed to streamline your experience. If you have any questions, don''t hesitate to reach out to our support team. Welcome aboard!',
 '["customer_name"]',
 true),

('Account Confirmation',
 'Hi {{customer_name}}, Your account has been successfully verified and is now fully active. You can now access all features and services available to your subscription tier. Thank you for choosing our platform. We look forward to supporting your needs.',
 '["customer_name"]',
 true),

('General Confirmation',
 'Hi {{customer_name}}, Thank you for your submission. We have received your request and it''s being processed. You will receive a confirmation notification once the process is complete. Ticket number: {{ticket_no}}. For reference, please save this ticket number. Our team is working on your request and will respond within the expected timeframe. If you have any questions or need to provide additional information, please reply to this ticket.',
 '["customer_name", "ticket_no"]',
 true),

('Maintenance Notification',
 'Hi {{customer_name}}, We wanted to inform you about scheduled maintenance. Scheduled maintenance window: {{maintenance_window}}. During this time, some features may be temporarily unavailable or may experience slower response times. Our team is working to improve our platform and bring you new features. We apologize for any inconvenience and appreciate your patience. All data is secure and will not be affected. Maintenance updates will be provided as needed. Thank you for your understanding.',
 '["customer_name", "maintenance_window"]',
 true),

('Feature Update Announcement',
 'Hi {{customer_name}}, We''re excited to announce a new feature: {{feature_name}}. {{feature_description}}. This feature is now available to all users. What''s new: {{benefits}}. How it helps: {{improvements}}. Check out our Help Center for detailed tutorials and guides. We believe this enhancement will significantly improve your experience. Your feedback is valuable - let us know what you think! If you have any questions or need assistance using this new feature, please create a support ticket. Happy exploring!',
 '["customer_name", "feature_name", "feature_description", "benefits", "improvements"]',
 true),

('Service Status Update',
 'Hi {{customer_name}}, Here''s an update on your request: Ticket #{{ticket_no}}. Status: {{status}}. Update: {{update_details}}. Expected resolution: {{resolution_timeline}}. Our team continues to work on your request. If you have additional information or questions, please reply directly to this ticket. We appreciate your patience and are committed to providing you with the best possible service.',
 '["customer_name", "ticket_no", "status", "update_details", "resolution_timeline"]',
 true),

('Thank You Message',
 'Hi {{customer_name}}, Thank you for contacting us. We appreciate the opportunity to assist you. If you have any further questions or if there''s anything else we can help you with, please don''t hesitate to reach out. Your feedback is important to us and helps us improve our services. If you were satisfied with our service, please consider leaving a review. We look forward to serving you again in the future.',
 '["customer_name"]',
 true),

-- BALANCED PRIORITY TEMPLATES (5)
('Critical Security Alert',
 'URGENT SECURITY NOTICE: Hi {{customer_name}}, We detected {{security_event}} on your account. Event details: {{event_details}}. Immediate action required: {{required_action}}. To protect your account: 1. Change your password immediately 2. Enable two-factor authentication if not already enabled 3. Review recent account activity 4. Report any suspicious activity to security@domain.com. If this was NOT you, please secure your account immediately. If this was you and you recognize this activity, no action is needed. Your account security is our top priority. Contact support if you need assistance.',
 '["customer_name", "security_event", "event_details", "required_action"]',
 true),

('Payment Failure Critical',
 'URGENT: Hi {{customer_name}}, Your payment attempt failed. Error: {{error_message}}. Amount: {{amount}} {{currency}}. Immediate action needed: 1. Verify payment method details are correct 2. Check for sufficient funds 3. Try alternative payment method 4. Contact support if issue persists. To avoid service interruption: Please resolve payment issue within 24 hours. Failure to resolve may result in service restrictions. We apologize for any inconvenience and are here to help.',
 '["customer_name", "error_message", "amount", "currency"]',
 true),

('System Outage Critical',
 'URGENT SYSTEM NOTIFICATION: Hi {{customer_name}}, We are currently experiencing a system outage. Affected services: {{affected_services}}. Status: {{current_status}}. Our team is working diligently to restore full service. Estimated resolution time: {{eta}}. What to expect: Limited or no access to affected services 2. Potential data synchronization delays 3. Workarounds if available (none at this time). We apologize for this disruption and appreciate your patience. Updates will be provided as soon as more information is available.',
 '["customer_name", "affected_services", "current_status", "eta"]',
 true),

('Monthly Newsletter',
 'Hi {{customer_name}}, Here''s your monthly newsletter. This month''s highlights: {{highlights}}. New features: {{new_features}}. Tips: {{tips}}. Upcoming events: {{events}}. Stay connected with us for the latest updates and insights. You can manage your newsletter preferences in your account settings. Thank you for being part of our community!',
 '["customer_name", "highlights", "new_features", "tips", "events"]',
 true),

('Product Announcement',
 'Hi {{customer_name}}, We''re excited to announce: {{announcement}}. What''s new: {{details}}. Why it matters: {{benefits}}. How it helps you: {{improvements}}. Availability: {{availability_date}}. Get started: Learn more in our Help Center or create a ticket for assistance. Your feedback helps us improve! We hope you enjoy this new addition to our platform.',
 '["customer_name", "announcement", "details", "benefits", "improvements", "availability_date"]',
 true);

-- =====================================================
-- INSERT CATEGORY RELATIONSHIPS INTO article_template_applicable_categories
-- =====================================================

INSERT INTO article_template_applicable_categories (article_template_id, category)
SELECT id, category
FROM (VALUES
-- PASSWORD & ACCOUNT TEMPLATES (10)
('Password Reset Template', 'ACCOUNT'),
('Account Unlock Notification', 'ACCOUNT'),
('2FA Setup Guide', 'ACCOUNT'),
('Profile Update Confirmation', 'ACCOUNT'),
('Email Change Verification', 'ACCOUNT'),
('Account Delete Warning', 'ACCOUNT'),
('Password Reset Success', 'ACCOUNT'),
('Tier Upgrade Confirmation', 'BILLING'),
('Address Update Success', 'ACCOUNT'),
('Tax ID Added', 'BILLING'),

-- BILLING & PAYMENT TEMPLATES (17)
('Payment Successful', 'BILLING'),
('Payment Failed', 'BILLING'),
('Invoice Ready', 'BILLING'),
('Subscription Renewed', 'BILLING'),
('Refund Initiated', 'BILLING'),
('Refund Approved', 'BILLING'),
('Payment Method Updated', 'BILLING'),
('Credit Applied', 'BILLING'),
('Payment Overdue', 'BILLING'),
('Auto-Payment Failed', 'BILLING'),
('Multi-Invoice Combined', 'BILLING'),
('Gift Card/Redeemption', 'BILLING'),
('Split Payment Success', 'BILLING'),
('Late Payment Fee Notification', 'BILLING'),
('Subscription Downgraded', 'BILLING'),
('Currency Changed', 'BILLING'),
('Discount Applied', 'BILLING'),

-- ORDER & SHIPPING TEMPLATES (17)
('Order Confirmation', 'SHIPPING'),
('Order Shipped', 'SHIPPING'),
('Order Delivered', 'SHIPPING'),
('Order Delayed', 'SHIPPING'),
('Out for Delivery', 'SHIPPING'),
('Pickup Ready', 'SHIPPING'),
('Shipping Method Changed', 'SHIPPING'),
('Return Label Generated', 'SHIPPING'),
('Partial Shipment', 'SHIPPING'),
('International Shipping', 'SHIPPING'),
('Address Update', 'SHIPPING'),
('Shipping Rate Applied', 'SHIPPING'),
('Carrier Update', 'SHIPPING'),
('Delivery Window', 'SHIPPING'),
('Residential Delivery', 'SHIPPING'),
('Business Delivery', 'SHIPPING'),
('Saturday Delivery', 'SHIPPING'),

-- TECHNICAL & ACCOUNT TEMPLATES (17)
('Ticket Created', 'TECHNICAL'),
('Ticket Updated', 'TECHNICAL'),
('Ticket Assigned', 'TECHNICAL'),
('Ticket Escalated', 'TECHNICAL'),
('Ticket Resolved', 'TECHNICAL'),
('Ticket Closed', 'TECHNICAL'),
('2FA Disabled', 'TECHNICAL'),
('API Key Generated', 'TECHNICAL'),
('API Key Rotated', 'TECHNICAL'),
('Device Added', 'TECHNICAL'),
('Session Terminated', 'TECHNICAL'),
('Password Changed', 'TECHNICAL'),
('Email Verified', 'TECHNICAL'),
('Profile Updated', 'TECHNICAL'),
('Company Profile Created', 'TECHNICAL'),
('Notification Preferences', 'TECHNICAL'),
('Account Merge', 'TECHNICAL'),

-- SECURITY TEMPLATES (10)
('Security Alert', 'SECURITY'),
('Password Compromised', 'SECURITY'),
('Phishing Warning', 'SECURITY'),
('New Device Login', 'SECURITY'),
('2FA Added', 'SECURITY'),
('Suspicious Activity Blocked', 'SECURITY'),
('API Key Expiring', 'SECURITY'),
('Data Export Complete', 'SECURITY'),
('Session Management Warning', 'SECURITY'),
('Security Check', 'SECURITY'),

-- GENERAL & SUPPORT TEMPLATES (15)
('Welcome Message', 'OTHER'),
('Ticket Response', 'OTHER'),
('Resolution Follow-up', 'OTHER'),
('Feature Announcement', 'OTHER'),
('Maintenance Notification', 'OTHER'),
('System Status', 'OTHER'),
('Privacy Policy Update', 'OTHER'),
('Terms Update', 'OTHER'),
('Account Verification', 'OTHER'),
('Feedback Request', 'OTHER'),
('Community Guidelines', 'OTHER'),
('Help Article Suggestion', 'OTHER'),
('Feature Tutorial', 'OTHER'),
('Quick Actions Reference', 'OTHER'),
-- Skip Beta Program Invitation, Survey Invitation (not in templates list)

-- ENRICHMENT - Additional Templates (18)
('Account Verification Required', 'ACCOUNT'),
('Profile Incomplete Warning', 'ACCOUNT'),
('Security Enhancement Required', 'ACCOUNT'),
('Payment Plan Update', 'BILLING'),
('Payment Retry Successful', 'BILLING'),
('Prorated Credit Applied', 'BILLING'),
('Shipping Label Download', 'SHIPPING'),
('Delivery Confirmation Request', 'SHIPPING'),
('Package Pickup Notification', 'SHIPPING'),
('Password Expiring Warning', 'TECHNICAL'),
('Session Timeout Warning', 'TECHNICAL'),
('Maintenance Completed', 'TECHNICAL'),
('Unusual Activity Alert', 'SECURITY'),
('Suspicious Login Blocked', 'SECURITY'),
('Device Trust Confirmation', 'SECURITY'),
('System Update Notification', 'OTHER'),
('Feature Feedback Request', 'OTHER'),

-- BALANCED PRIORITY TEMPLATES (5)
('Critical Security Alert', 'SECURITY'),
('Payment Failure Critical', 'BILLING'),
('System Outage Critical', 'TECHNICAL'),
('Monthly Newsletter', 'OTHER'),
('Product Announcement', 'OTHER')) AS v(name, category)
       JOIN article_template at ON at.name = v.name;

-- =====================================================
-- INSERT PRIORITY RELATIONSHIPS INTO article_template_applicable_priorities
-- =====================================================

INSERT INTO article_template_applicable_priorities (article_template_id, priority)
SELECT id, priority
FROM (VALUES
-- CRITICAL Priority Templates (3)
('Critical Security Alert', 'CRITICAL'),
('Payment Failure Critical', 'CRITICAL'),
('System Outage Critical', 'CRITICAL'),

-- LOW Priority Templates (2)
('Monthly Newsletter', 'LOW'),
('Product Announcement', 'LOW')) AS v(name, priority)
       JOIN article_template at ON at.name = v.name;

COMMIT;
