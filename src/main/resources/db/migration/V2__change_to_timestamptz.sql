-- Change created_at column to have time zone information
ALTER TABLE messages ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;
