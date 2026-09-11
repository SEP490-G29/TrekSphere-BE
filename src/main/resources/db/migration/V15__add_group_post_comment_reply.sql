-- Migration V15: Support 2-level reply comments in group workspace feed
ALTER TABLE group_post_comment
    ADD COLUMN parent_comment_id UUID NULL,
    ADD COLUMN reply_to_comment_id UUID NULL,
    ADD COLUMN reply_to_member_id UUID NULL;

ALTER TABLE group_post_comment
    ADD CONSTRAINT fk_gpc_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES group_post_comment(group_post_comment_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_gpc_reply_to_comment FOREIGN KEY (reply_to_comment_id) REFERENCES group_post_comment(group_post_comment_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_gpc_reply_to_member FOREIGN KEY (reply_to_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL;

CREATE INDEX ix_gpc_parent_comment ON group_post_comment (parent_comment_id, created_at ASC);
CREATE INDEX ix_gpc_reply_to_comment ON group_post_comment (reply_to_comment_id);
