-- V2__seed_master_data.sql
-- Master data cho bảng role, permission, role_permission

-- 1. Bảng ROLE
INSERT INTO role (role_id, role_name, description) VALUES
(gen_random_uuid(), 'ADMIN', 'Quản trị viên hệ thống'),
(gen_random_uuid(), 'TREKKER', 'Khách hàng sử dụng dịch vụ trekking'),
(gen_random_uuid(), 'VENDOR_MANAGER', 'Quản lý đối tác du lịch'),
(gen_random_uuid(), 'VENDOR_STAFF', 'Nhân viên của đối tác du lịch'),
(gen_random_uuid(), 'COORDINATOR', 'Điều phối viên / Hướng dẫn viên');

-- 2. Bảng PERMISSION
INSERT INTO permission (permission_id, resource, action, description) VALUES
(gen_random_uuid(), 'TOUR', 'CREATE', 'Quyền tạo mới tour'),
(gen_random_uuid(), 'TOUR', 'READ', 'Quyền xem danh sách/chi tiết tour'),
(gen_random_uuid(), 'TOUR', 'UPDATE', 'Quyền cập nhật tour'),
(gen_random_uuid(), 'TOUR', 'DELETE', 'Quyền xóa/ẩn tour'),

(gen_random_uuid(), 'BOOKING', 'CREATE', 'Quyền đặt tour'),
(gen_random_uuid(), 'BOOKING', 'READ', 'Quyền xem đơn đặt tour'),
(gen_random_uuid(), 'BOOKING', 'UPDATE', 'Quyền cập nhật trạng thái đơn đặt'),

(gen_random_uuid(), 'USER', 'READ', 'Quyền xem thông tin người dùng'),
(gen_random_uuid(), 'USER', 'UPDATE', 'Quyền cập nhật thông tin người dùng');

-- 3. Bảng ROLE_PERMISSION
-- Phân quyền cho ADMIN (Tất cả quyền)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r, permission p
WHERE r.role_name = 'ADMIN';

-- Phân quyền cho VENDOR_MANAGER (Quản lý Tour)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r, permission p
WHERE r.role_name = 'VENDOR_MANAGER' 
  AND p.resource IN ('TOUR', 'BOOKING');

-- Phân quyền cho TREKKER (Chỉ đọc Tour, tạo Booking, cập nhật User)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r, permission p
WHERE r.role_name = 'TREKKER' 
  AND (
    (p.resource = 'TOUR' AND p.action = 'READ') OR
    (p.resource = 'BOOKING' AND p.action IN ('CREATE', 'READ')) OR
    (p.resource = 'USER' AND p.action = 'UPDATE')
  );
