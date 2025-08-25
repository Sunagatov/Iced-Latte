--liquibase formatted sql

--changeset [author]:[changeset-id]
--comment: [Brief description of what this migration does]
--author: [author-name]
--date: [YYYY-MM-DD]
--labels: [optional-labels]
--preconditions onFail:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '[table_name]'

-- Migration Description:
-- [Detailed description of the changes being made]
-- 
-- Impact:
-- - [List any potential impacts]
-- - [Performance considerations]
-- - [Data migration notes]
--
-- Rollback Strategy:
-- [Describe how to rollback if needed]

-- ========== MIGRATION STARTS HERE ==========

-- [Your SQL statements here]

-- Example:
-- CREATE TABLE example_table (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     name VARCHAR(255) NOT NULL,
--     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
-- );

-- CREATE INDEX idx_example_table_name ON example_table(name);

-- ========== MIGRATION ENDS HERE ==========

--rollback DROP TABLE IF EXISTS [table_name];