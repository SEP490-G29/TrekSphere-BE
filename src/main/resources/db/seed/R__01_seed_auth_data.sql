-- R__seed_auth_data.sql
-- Seed Data for Authentication & Authorization

INSERT INTO role (roleid, role_name, description)
VALUES
    (gen_random_uuid(), 'ADMIN',          'Quản trị viên hệ thống TrekSphere'),
    (gen_random_uuid(), 'TREKKER',        'Người dùng thông thường (Khách hàng)'),
    (gen_random_uuid(), 'VENDOR_MANAGER', 'Quản lý của nhà cung cấp tour'),
    (gen_random_uuid(), 'VENDOR_STAFF',   'Nhân viên của nhà cung cấp'),
    (gen_random_uuid(), 'COORDINATOR',    'Điều phối viên quản lý các nhóm/chuyến đi'),
    (gen_random_uuid(), 'PORTER',         'Người khuân vác, hỗ trợ chuyến đi')
ON CONFLICT (role_name) DO NOTHING;

-- Mật khẩu chung: Pass123@
-- BCrypt Hash (Cost 10): $2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK

INSERT INTO users (
    userid, email, password_hash, full_name, status,
    email_verified, is_deleted, created_at, phone
)
VALUES 
    -- Admin
    (
        '11111111-1111-1111-1111-111111111111', 'admin@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Hệ Thống Admin', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0900000000'
    ),
    -- Trekkers
    (
        '22222222-2222-2222-2222-222222222221', 'trekker1@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Nguyễn Văn An', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0901111111'
    ),
    (
        '22222222-2222-2222-2222-222222222222', 'trekker2@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Trần Thị Bích', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0902222222'
    ),
    -- Vendor Managers
    (
        '33333333-3333-3333-3333-333333333331', 'vendor1@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Lê Hoàng Cường', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0903333333'
    ),
    (
        '33333333-3333-3333-3333-333333333332', 'vendor2@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Phạm Xuân Dũng', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0904444444'
    ),
    -- Vendor Staff
    (
        '44444444-4444-4444-4444-444444444441', 'staff1@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Vũ Minh Em', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0905555555'
    ),
    -- Coordinator
    (
        '55555555-5555-5555-5555-555555555551', 'coordinator1@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Đặng Quốc Phong', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0906666666'
    ),
    -- Porters
    (
        '66666666-6666-6666-6666-666666666661', 'porter1@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Bàn Văn Tòng', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0907777777'
    ),
    (
        '66666666-6666-6666-6666-666666666662', 'porter2@treksphere.com',
        '$2a$10$mTZWyUlUySnAmCHENymzZekxhbWF3VqDcyoWPPyI8.sHQX.irL/wK', 'Giàng A Lử', 'ACTIVE', true, false, CURRENT_TIMESTAMP, '0908888888'
    )
ON CONFLICT (email) DO NOTHING;

-- Cấp quyền (Mapping Role)
-- Xóa bảng user_role cho các user này để tránh lỗi Duplicate nếu đã cấp
DELETE FROM user_role WHERE user_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222221',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333331',
    '33333333-3333-3333-3333-333333333332',
    '44444444-4444-4444-4444-444444444441',
    '55555555-5555-5555-5555-555555555551',
    '66666666-6666-6666-6666-666666666661',
    '66666666-6666-6666-6666-666666666662'
);

INSERT INTO user_role (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', roleid FROM role WHERE role_name = 'ADMIN' UNION ALL
SELECT '22222222-2222-2222-2222-222222222221', roleid FROM role WHERE role_name = 'TREKKER' UNION ALL
SELECT '22222222-2222-2222-2222-222222222222', roleid FROM role WHERE role_name = 'TREKKER' UNION ALL
SELECT '33333333-3333-3333-3333-333333333331', roleid FROM role WHERE role_name = 'VENDOR_MANAGER' UNION ALL
SELECT '33333333-3333-3333-3333-333333333332', roleid FROM role WHERE role_name = 'VENDOR_MANAGER' UNION ALL
SELECT '44444444-4444-4444-4444-444444444441', roleid FROM role WHERE role_name = 'VENDOR_STAFF' UNION ALL
SELECT '55555555-5555-5555-5555-555555555551', roleid FROM role WHERE role_name = 'COORDINATOR' UNION ALL
SELECT '66666666-6666-6666-6666-666666666661', roleid FROM role WHERE role_name = 'PORTER' UNION ALL
SELECT '66666666-6666-6666-6666-666666666662', roleid FROM role WHERE role_name = 'PORTER'
ON CONFLICT DO NOTHING;
