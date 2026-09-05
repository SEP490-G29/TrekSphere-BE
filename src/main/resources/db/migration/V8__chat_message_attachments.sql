ALTER TABLE message
    ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN attachment_storage_id VARCHAR(500),
    ADD COLUMN attachment_url VARCHAR(1000),
    ADD COLUMN attachment_name VARCHAR(255),
    ADD COLUMN attachment_mime_type VARCHAR(255),
    ADD COLUMN attachment_size_bytes BIGINT;

ALTER TABLE message ALTER COLUMN content DROP NOT NULL;

ALTER TABLE message
    ADD CONSTRAINT chk_message_type
        CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),
    ADD CONSTRAINT chk_message_payload CHECK (
        (message_type IN ('TEXT', 'SYSTEM')
            AND NULLIF(BTRIM(content), '') IS NOT NULL
            AND attachment_url IS NULL)
        OR
        (message_type IN ('IMAGE', 'FILE')
            AND attachment_url IS NOT NULL
            AND attachment_name IS NOT NULL
            AND attachment_mime_type IS NOT NULL
            AND attachment_size_bytes > 0)
    );

