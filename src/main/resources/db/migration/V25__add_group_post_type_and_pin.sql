-- Add post_type, is_pinned and pinned_at to group_post table
ALTER TABLE group_post
    ADD COLUMN IF NOT EXISTS post_type VARCHAR(30) NOT NULL DEFAULT 'DISCUSSION',
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_group_post_pinned_created 
    ON group_post (matching_group_id, is_pinned DESC, created_at DESC);
