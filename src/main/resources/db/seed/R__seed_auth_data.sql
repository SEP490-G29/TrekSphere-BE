-- R__seed_auth_data.sql
-- Seed Data for Authentication & Authorization

INSERT INTO role (roleid, role_name, description)
VALUES
    (gen_random_uuid(), 'ADMIN',          'Quản trị viên hệ thống TrekSphere'),
    (gen_random_uuid(), 'TREKKER',        'Người dùng thông thường'),
    (gen_random_uuid(), 'VENDOR_MANAGER', 'Quản lý của nhà cung cấp'),
    (gen_random_uuid(), 'VENDOR_STAFF',   'Nhân viên của nhà cung cấp');


INSERT INTO users (
    userid, email, password_hash, full_name, status,
    email_verified, is_deleted, created_at
)
VALUES (
           '11111111-1111-1111-1111-111111111111',
           'admin@treksphere.com',
           '$2a$12$U9wlqfpWBI/sMn3T.Ukzcue8P4o.mIVcSdSkNoTiYE9eyA4InyMqC', --admin
           'System Administrator',
           'ACTIVE',
           true,
           false,
           CURRENT_TIMESTAMP
       );

INSERT INTO user_role (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', roleid
FROM role
WHERE role_name = 'ADMIN';