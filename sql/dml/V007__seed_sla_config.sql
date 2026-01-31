INSERT INTO policy_config (config_key, config_value, value_type, description, active, default_value)
VALUES 
  ('SLA_CUSTOMER_RESPONSE_HOURS', '48', 'INTEGER', 'Hours to wait for customer response before SLA breach', true, '48'),
  ('SLA_AGENT_RESPONSE_HOURS', '24', 'INTEGER', 'Hours before agent response SLA breach', true, '24'),
  ('AUTO_CLOSE_WARNING_DAYS', '3', 'INTEGER', 'Days of inactivity before auto-close warning', true, '3'),
  ('AUTO_CLOSE_FINAL_DAYS', '7', 'INTEGER', 'Days of inactivity before auto-close resolved tickets', true, '7'),
  ('AUTO_ROUTE_THRESHOLD', '0.70', 'DOUBLE', 'Minimum confidence threshold for auto-routing', true, '0.70'),
  ('CRITICAL_MIN_CONF', '0.85', 'DOUBLE', 'Minimum confidence for CRITICAL priority tickets', true, '0.85');
