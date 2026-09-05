ALTER TABLE tour
    ADD COLUMN published_at TIMESTAMP,
    ADD COLUMN hidden_reason TEXT,
    ADD COLUMN hidden_at TIMESTAMP,
    ADD COLUMN hidden_by UUID;

ALTER TABLE tour
    ADD CONSTRAINT fk_tour_hidden_by FOREIGN KEY (hidden_by) REFERENCES users(user_id);

UPDATE tour
SET published_at = COALESCE(updated_at, created_at)
WHERE status = 'APPROVED';

UPDATE tour
SET hidden_reason = rejection_reason,
    hidden_at = COALESCE(updated_at, created_at)
WHERE status = 'HIDDEN';

UPDATE tour SET status = 'PUBLISHED' WHERE status = 'APPROVED';
UPDATE tour SET status = 'DRAFT' WHERE status IN ('PENDING_APPROVAL', 'REJECTED');

ALTER TABLE tour DROP CONSTRAINT chk_tour_status;
ALTER TABLE tour
    ADD CONSTRAINT chk_tour_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN'));

CREATE INDEX ix_tour_vendor_status_active
    ON tour (vendor_id, status, is_deleted);
CREATE INDEX ix_tour_published_at
    ON tour (published_at DESC)
    WHERE status = 'PUBLISHED' AND is_deleted = FALSE;
CREATE INDEX ix_tour_schedule_tour_status_departure
    ON tour_schedule (tour_id, status, departure_date, is_deleted);
CREATE INDEX ix_matching_group_tour_status_active
    ON matching_group (tour_id, status, is_deleted);

ALTER TABLE tour_schedule
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN cancelled_at TIMESTAMP;

UPDATE tour_schedule
SET cancelled_at = COALESCE(updated_at, created_at)
WHERE status = 'CANCELLED';
