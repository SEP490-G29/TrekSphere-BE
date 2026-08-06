ALTER TABLE matching_group
    ADD CONSTRAINT chk_matching_group_current_size
    CHECK (current_size >= 1 AND current_size <= max_size);
