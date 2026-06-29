-- V2__seed_data.sql
-- Seed Data for Authentication & Authorization


INSERT INTO roles (role_id, role_name, description, created_at)
VALUES 
    (gen_random_uuid(), 'ADMIN', 'Quản trị viên hệ thống TrekSphere', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TREKKER', 'Người dùng thông thường (Trekker)', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'VENDOR_MANAGER', 'Quản lý của Vendor (đối tác)', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'VENDOR_STAFF', 'Nhân viên của Vendor', CURRENT_TIMESTAMP);


INSERT INTO users (user_id, email, password_hash, full_name, status, email_verified, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111', 
    'admin@treksphere.com', 
    '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1UxgNhCAW9BExAJe', 
    'System Administrator', 
    'ACTIVE', 
    true, 
    CURRENT_TIMESTAMP
);


INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', role_id 
FROM roles 
WHERE role_name = 'ADMIN';
