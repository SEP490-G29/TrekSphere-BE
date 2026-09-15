-- Bổ sung theo dõi tiến độ check-in cho điểm dừng (checkpoint) trong hành trình Custom Journey
ALTER TABLE custom_journey_checkpoint
    ADD COLUMN is_checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN checked_in_at TIMESTAMP NULL,
    ADD COLUMN checked_in_by_member_id UUID NULL,
    ADD CONSTRAINT fk_checkpoint_checked_in_by_member
        FOREIGN KEY (checked_in_by_member_id) REFERENCES matching_member(matching_member_id) ON DELETE SET NULL;
