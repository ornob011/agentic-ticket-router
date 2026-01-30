-- =====================================================
-- Add LLM Output Type Enum and Column
-- =====================================================

-- Drop existing type if exists (for migration reruns)
DROP TYPE IF EXISTS llm_output_type CASCADE;

-- Create LLM output type enum
CREATE TYPE llm_output_type AS ENUM (
  'ROUTING',
  'ANALYSIS_TICKET_DETAILS',
  'ANALYSIS_CUSTOMER_INFO',
  'ANALYSIS_CONVERSATION_HISTORY',
  'ANALYSIS_TECHNICAL_DETAILS',
  'ANALYSIS_ACTIONS_REQUIRED'
);

-- Add output_type column to llm_output table
ALTER TABLE llm_output
ADD COLUMN output_type llm_output_type NOT NULL DEFAULT 'ROUTING';

-- Create index for queries by output type
CREATE INDEX idx_llm_output_type ON llm_output(output_type);

-- Comment on the new type
COMMENT ON TYPE llm_output_type IS 'Type of LLM output: ROUTING (for routing decisions) or ANALYSIS_* (for ticket section analysis)';
COMMENT ON COLUMN llm_output.output_type IS 'Classification of the LLM output to distinguish between routing decisions and ticket analysis results';
