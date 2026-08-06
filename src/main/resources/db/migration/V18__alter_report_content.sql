-- V18__alter_report_content.sql
-- Thêm cột resolution_notes và resolved_by cho bảng report_content

ALTER TABLE report_content
ADD COLUMN resolution_notes VARCHAR(500),
ADD COLUMN resolved_by UUID;

ALTER TABLE report_content
ADD CONSTRAINT fk_rc_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(user_id);
