-- Phase 4 của hệ thống notification (draft/notification_system_plan.md) thêm 4 giá trị ReferenceType
-- mới vào Java enum (VENDOR_APPLICATION, VENDOR, REPORT, USER) nhưng CHECK constraint gốc ở
-- V1__init_schema.sql chỉ liệt kê 8 giá trị ban đầu. Không nới constraint sẽ khiến mọi notification
-- dùng 1 trong 4 giá trị mới bị insert lỗi (bị nuốt âm thầm bởi try/catch trong NotificationEventListener).
ALTER TABLE notification DROP CONSTRAINT chk_notification_reference_type;

ALTER TABLE notification ADD CONSTRAINT chk_notification_reference_type CHECK (
    reference_type IS NULL OR reference_type IN
    ('TOUR','BLOG','MATCHING_GROUP','CONVERSATION','GROUP_TRIP','GROUP_EXPENSE','GROUP_VOTE','SOS',
     'VENDOR_APPLICATION','VENDOR','REPORT','USER')
);
