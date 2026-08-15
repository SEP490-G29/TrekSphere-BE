ALTER TABLE session_equipment
    ADD COLUMN returned_quantity INTEGER,
    ADD COLUMN missing_quantity INTEGER,
    ADD COLUMN return_status VARCHAR(30) NOT NULL DEFAULT 'NOT_RETURNED',
    ADD COLUMN submitted_by UUID,
    ADD COLUMN submitted_at TIMESTAMP,
    ADD COLUMN confirmed_by UUID,
    ADD COLUMN confirmed_at TIMESTAMP;

ALTER TABLE session_equipment
    ADD CONSTRAINT fk_se_submitted FOREIGN KEY (submitted_by) REFERENCES users(user_id),
    ADD CONSTRAINT fk_se_confirmed FOREIGN KEY (confirmed_by) REFERENCES users(user_id);
