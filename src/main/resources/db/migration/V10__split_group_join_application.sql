-- Migration V10: Split group_join_application and matching_member to preserve complete application history

-- 1. Create group_join_application table
CREATE TABLE group_join_application (
    application_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    applicant_user_id UUID NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    reject_reason TEXT,
    withdrawn_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gja_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT fk_gja_applicant FOREIGN KEY (applicant_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_gja_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT chk_gja_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_gja_withdrawn_at CHECK (
        (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
        OR (status <> 'WITHDRAWN' AND withdrawn_at IS NULL)
    )
);

-- Unique index: At most 1 PENDING application per user per group
CREATE UNIQUE INDEX uq_group_applicant_pending
    ON group_join_application (matching_group_id, applicant_user_id)
    WHERE status = 'PENDING' AND is_deleted = FALSE;

CREATE INDEX ix_gja_group_status ON group_join_application (matching_group_id, status);
CREATE INDEX ix_gja_applicant_status ON group_join_application (applicant_user_id, status);

-- 2. Update matching_member table to link with source application and track membership lifecycle
ALTER TABLE matching_member
    ADD COLUMN source_application_id UUID,
    ADD COLUMN joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN left_at TIMESTAMP;

ALTER TABLE matching_member
    ADD CONSTRAINT fk_mm_source_application FOREIGN KEY (source_application_id) REFERENCES group_join_application(application_id);
