-- Drop table-level UNIQUE constraint on custom_journey_checkpoint
-- which erroneously enforces uniqueness across soft-deleted records.
ALTER TABLE custom_journey_checkpoint DROP CONSTRAINT IF EXISTS uq_cjc_order;

-- Create partial unique index enforcing checkpoint order uniqueness only among active (non-deleted) records.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cjc_order_active
    ON custom_journey_checkpoint (custom_journey_id, checkpoint_order)
    WHERE is_deleted = FALSE;
