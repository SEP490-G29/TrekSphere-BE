-- Migration V4: Add missing matching group schema fields and tables according to db_refactor_v3.md (38-table target)

-- 1. Add difficulty column to custom_journey
ALTER TABLE custom_journey 
ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'MODERATE',
ADD CONSTRAINT chk_cj_difficulty CHECK (difficulty IN ('EASY','MODERATE','HARD','EXTREME'));

-- 2. Add scheduled_start_at column to group_trip
ALTER TABLE group_trip 
ADD COLUMN scheduled_start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 3. Create group_emergency_contact table (38th table in target schema)
CREATE TABLE group_emergency_contact (
    group_emergency_contact_id UUID PRIMARY KEY,
    matching_group_id UUID NOT NULL,
    contact_type VARCHAR(30) NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    note TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_gec_group FOREIGN KEY (matching_group_id) REFERENCES matching_group(matching_group_id),
    CONSTRAINT chk_gec_contact_type CHECK (contact_type IN ('NATIONWIDE', 'OTHER'))
);

CREATE INDEX ix_gec_group_active ON group_emergency_contact (matching_group_id, is_active);
