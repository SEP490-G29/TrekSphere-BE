-- Thay boolean is_checked_in bằng trạng thái tiến độ 3 giá trị (PENDING/CHECKED_IN/SKIPPED)
-- để hỗ trợ leader "Bỏ qua" 1 checkpoint, không chỉ check-in.
ALTER TABLE custom_journey_checkpoint
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

UPDATE custom_journey_checkpoint
    SET status = 'CHECKED_IN'
    WHERE is_checked_in = TRUE;

ALTER TABLE custom_journey_checkpoint
    DROP COLUMN is_checked_in;

ALTER TABLE custom_journey_checkpoint
    RENAME COLUMN checked_in_at TO progress_updated_at;

ALTER TABLE custom_journey_checkpoint
    RENAME COLUMN checked_in_by_member_id TO progress_updated_by_member_id;
