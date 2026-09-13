-- Migration V17: Refactor from legacy group_moment to unified moment and moment_media tables

-- 1. Drop legacy unused group_moment tables if they exist from V1
DROP TABLE IF EXISTS group_moment_media CASCADE;
DROP TABLE IF EXISTS group_moment CASCADE;

-- 2. Create unified moment table
CREATE TABLE IF NOT EXISTS moment (
    moment_id UUID PRIMARY KEY,
    author_user_id UUID NOT NULL,
    matching_group_id UUID,
    author_matching_member_id UUID,
    caption TEXT,
    captured_at TIMESTAMP,
    place_name VARCHAR(200),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    visibility VARCHAR(20) NOT NULL DEFAULT 'GROUP_ONLY',
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    hidden_by_user_id UUID,
    hidden_reason TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_moment_author_user FOREIGN KEY (author_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_matching_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id) ON DELETE SET NULL,
    CONSTRAINT fk_moment_author_member FOREIGN KEY (author_matching_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL,
    CONSTRAINT fk_moment_hidden_by FOREIGN KEY (hidden_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_moment_visibility CHECK (visibility IN ('GROUP_ONLY', 'PUBLIC_PROFILE', 'ONLY_ME')),
    CONSTRAINT chk_moment_status CHECK (status IN ('VISIBLE', 'HIDDEN'))
);

-- 3. Create moment_media table
CREATE TABLE IF NOT EXISTS moment_media (
    moment_media_id UUID PRIMARY KEY,
    moment_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_moment_media_moment FOREIGN KEY (moment_id) REFERENCES moment(moment_id) ON DELETE CASCADE,
    CONSTRAINT uq_moment_media_order UNIQUE (moment_id, sort_order)
);

-- 4. Create indexes
CREATE INDEX IF NOT EXISTS ix_moment_group ON moment (matching_group_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_moment_author ON moment (author_user_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_moment_created_at ON moment (created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS ix_moment_media_moment ON moment_media (moment_id);
