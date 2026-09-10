CREATE TABLE custom_journey_activity (
    custom_journey_activity_id UUID PRIMARY KEY,
    custom_journey_id UUID NOT NULL,
    custom_journey_checkpoint_id UUID,
    day_no INTEGER NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    activity_order INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    planned_start_at VARCHAR(50),
    planned_end_at VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),

    CONSTRAINT fk_cja_journey FOREIGN KEY (custom_journey_id) REFERENCES custom_journey(custom_journey_id) ON DELETE CASCADE,
    CONSTRAINT fk_cja_checkpoint FOREIGN KEY (custom_journey_checkpoint_id) REFERENCES custom_journey_checkpoint(custom_journey_checkpoint_id) ON DELETE SET NULL,
    CONSTRAINT chk_cja_day_no CHECK (day_no > 0),
    CONSTRAINT chk_cja_activity_order CHECK (activity_order > 0),
    CONSTRAINT chk_cja_time_slot CHECK (time_slot IN ('MORNING', 'NOON', 'AFTERNOON', 'EVENING'))
);

CREATE INDEX ix_cja_journey_day_slot ON custom_journey_activity (custom_journey_id, day_no, time_slot) WHERE is_deleted = FALSE;
