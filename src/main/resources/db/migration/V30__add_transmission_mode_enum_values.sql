-- Add lowercase enum values to transmission_mode, if not already present
ALTER TYPE transmission_mode ADD VALUE IF NOT EXISTS 'value';
ALTER TYPE transmission_mode ADD VALUE IF NOT EXISTS 'reference';
