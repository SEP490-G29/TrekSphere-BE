ALTER TABLE booking_participant 
ADD COLUMN is_present_start BOOLEAN,
ADD COLUMN start_attended_at TIMESTAMP,
ADD COLUMN is_present_end BOOLEAN,
ADD COLUMN end_attended_at TIMESTAMP;
