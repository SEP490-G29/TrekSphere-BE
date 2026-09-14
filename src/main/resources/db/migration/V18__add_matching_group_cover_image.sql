-- Migration V18: Add cover_image_url column to matching_group table

ALTER TABLE matching_group
    ADD COLUMN cover_image_url VARCHAR(500);
