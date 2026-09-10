-- Migration V11: Add group_post_image table for supporting multiple images attached to group posts

CREATE TABLE group_post_image (
    group_post_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,

    CONSTRAINT fk_gpi_group_post FOREIGN KEY (group_post_id) REFERENCES group_post(group_post_id) ON DELETE CASCADE
);

CREATE INDEX ix_gpi_post_id ON group_post_image (group_post_id);
