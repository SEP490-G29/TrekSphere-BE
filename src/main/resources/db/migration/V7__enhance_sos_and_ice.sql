-- ============================================================================
-- Migration V7: Enhance SOS Alert with responding status/responder & User ICE contact
-- ============================================================================

-- 1. Add responder_id and responded_at to sos_alert table
ALTER TABLE sos_alert ADD COLUMN IF NOT EXISTS responder_id UUID;
ALTER TABLE sos_alert ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_sos_responder'
    ) THEN
        ALTER TABLE sos_alert
            ADD CONSTRAINT fk_sos_responder FOREIGN KEY (responder_id) REFERENCES users(user_id);
    END IF;
END $$;

-- 2. Update status check constraint on sos_alert to include RESPONDING
ALTER TABLE sos_alert DROP CONSTRAINT IF EXISTS chk_sos_status;
ALTER TABLE sos_alert ADD CONSTRAINT chk_sos_status CHECK (status IN ('OPEN','RESPONDING','ACKNOWLEDGED','RESOLVED','CANCELLED'));

-- 3. Add emergency contact fields to users table (ICE)
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20);
