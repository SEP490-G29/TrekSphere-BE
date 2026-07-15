-- V3__seed_users.sql
-- Master data cho bảng users, user_role
-- Mật khẩu mặc định: Pass123@ (được hash BCrypt)

-- 1. Bảng USERS
INSERT INTO users (user_id, email, password_hash, full_name, phone, status, is_deleted) VALUES
-- Admin
(gen_random_uuid(), 'admin@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'System Admin', '0901000000', 'ACTIVE', false),

-- Vendor Managers
(gen_random_uuid(), 'vendor1@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Nguyễn Văn Vendor 1', '0902000001', 'ACTIVE', false),
(gen_random_uuid(), 'vendor2@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Trần Thị Vendor 2', '0902000002', 'ACTIVE', false),

-- Vendor Staff
(gen_random_uuid(), 'staff1@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Lê Nhân Viên 1', '0903000001', 'ACTIVE', false),
(gen_random_uuid(), 'staff2@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Phạm Nhân Viên 2', '0903000002', 'ACTIVE', false),

-- Coordinators
(gen_random_uuid(), 'coordinator1@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Hoàng Điều Phối 1', '0904000001', 'ACTIVE', false),
(gen_random_uuid(), 'coordinator2@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Bùi Điều Phối 2', '0904000002', 'ACTIVE', false),

-- Trekkers
(gen_random_uuid(), 'trekker1@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Trekker Một', '0905000001', 'ACTIVE', false),
(gen_random_uuid(), 'trekker2@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Trekker Hai', '0905000002', 'ACTIVE', false),
(gen_random_uuid(), 'trekker3@treksphere.com', '$2a$12$qL2.IVyYCu4WP6iLQ0Rs6urFCBWLBlQP1EqEIx1L1DZltysrTmrru', 'Trekker Ba', '0905000003', 'ACTIVE', false);

-- 2. Bảng USER_ROLE
-- Gán Admin
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.email = 'admin@treksphere.com' AND r.role_name = 'ADMIN';

-- Gán Vendor Manager
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.email LIKE 'vendor%@treksphere.com' AND r.role_name = 'VENDOR_MANAGER';

-- Gán Vendor Staff
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.email LIKE 'staff%@treksphere.com' AND r.role_name = 'VENDOR_STAFF';

-- Gán Coordinator
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.email LIKE 'coordinator%@treksphere.com' AND r.role_name = 'COORDINATOR';

-- Gán Trekker
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.email LIKE 'trekker%@treksphere.com' AND r.role_name = 'TREKKER';
