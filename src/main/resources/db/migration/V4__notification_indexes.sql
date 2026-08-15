CREATE INDEX IF NOT EXISTS idx_notification_recipient_created_at
    ON notification (recipient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_read_created_at
    ON notification (recipient_id, is_read, created_at DESC)
    WHERE is_deleted = FALSE;
