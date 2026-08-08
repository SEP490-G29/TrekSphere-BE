-- Add refund bank info and refund proof image columns to booking table
ALTER TABLE booking ADD COLUMN refund_bank_name VARCHAR(100);
ALTER TABLE booking ADD COLUMN refund_account_number VARCHAR(50);
ALTER TABLE booking ADD COLUMN refund_account_holder VARCHAR(255);
ALTER TABLE booking ADD COLUMN refund_proof_image_url VARCHAR(500);
