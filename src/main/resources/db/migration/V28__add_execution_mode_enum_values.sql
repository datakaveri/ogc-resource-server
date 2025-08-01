-- Adds new values to the execution_mode enum type
ALTER TYPE execution_mode ADD VALUE IF NOT EXISTS 'sync-execute';
ALTER TYPE execution_mode ADD VALUE IF NOT EXISTS 'async-execute';
ALTER TYPE execution_mode ADD VALUE IF NOT EXISTS 'dismiss';