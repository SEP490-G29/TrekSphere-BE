-- V11__seed_coordinators_to_vendor_staff.sql
-- Seed dữ liệu mẫu gán Hướng dẫn viên (Coordinator) vào bảng nhân sự vendor_staff

-- 1. Gán Hoàng Điều Phối 1 (coordinator1@treksphere.com) vào Viettrekking
INSERT INTO vendor_staff (vendor_staff_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Viettrekking' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'coordinator1@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);

-- 2. Gán Bùi Điều Phối 2 (coordinator2@treksphere.com) vào Tổ Ong Adventure
INSERT INTO vendor_staff (vendor_staff_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Tổ Ong Adventure' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'coordinator2@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);
