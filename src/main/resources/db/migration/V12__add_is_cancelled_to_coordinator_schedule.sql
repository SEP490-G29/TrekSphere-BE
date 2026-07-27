-- V12__add_is_cancelled_to_coordinator_schedule.sql
-- Thêm cột is_cancelled vào bảng coordinator_schedule

ALTER TABLE coordinator_schedule 
ADD COLUMN is_cancelled BOOLEAN NOT NULL DEFAULT FALSE;
