-- Drop table-level UNIQUE constraint on group_expense_share
-- which erroneously enforces uniqueness across soft-deleted records.
ALTER TABLE group_expense_share DROP CONSTRAINT IF EXISTS uq_ges_expense_member;

-- Create partial unique index enforcing expense-member uniqueness only among active (non-deleted) records.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ges_expense_member_active
    ON group_expense_share (group_expense_id, matching_member_id)
    WHERE is_deleted = FALSE;
