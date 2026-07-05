-- R__02_seed_vendors.sql
-- Seed Data for Vendor, Vendor Staff, Application, Policy, and Permissions

-- 1. System Policy
INSERT INTO system_policy (policyid, config_key, config_value, data_type, description, is_deleted, created_at)
VALUES 
    (gen_random_uuid(), 'PLATFORM_FEE_PERCENTAGE', '5', 'INTEGER', 'Phí nền tảng thu từ Vendor', false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MIN_BOOKING_DAYS_ADVANCE', '3', 'INTEGER', 'Số ngày tối thiểu để đặt tour trước khi khởi hành', false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MAX_PENDING_PAYMENT_HOURS', '24', 'INTEGER', 'Số giờ tối đa chờ thanh toán trước khi hủy booking', false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MAX_REFUND_DAYS', '7', 'INTEGER', 'Số ngày tối đa để xử lý hoàn tiền', false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MAX_UPLOAD_SIZE_MB', '10', 'INTEGER', 'Kích thước file tối đa cho phép tải lên (MB)', false, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

-- 2. Permission
INSERT INTO permission (permissionid, resource, action, description)
VALUES 
    (gen_random_uuid(), 'TOUR', 'CREATE', 'Tạo tour mới'),
    (gen_random_uuid(), 'TOUR', 'UPDATE', 'Cập nhật tour'),
    (gen_random_uuid(), 'TOUR', 'DELETE', 'Xóa tour'),
    (gen_random_uuid(), 'BOOKING', 'CREATE', 'Tạo booking mới'),
    (gen_random_uuid(), 'BOOKING', 'VIEW_ALL', 'Xem tất cả booking'),
    (gen_random_uuid(), 'VENDOR', 'MANAGE', 'Quản lý thông tin vendor')
ON CONFLICT (resource, action) DO NOTHING;

-- 3. Vendor Application
INSERT INTO vendor_application (
    applicationid, applicant_user_id, company_name, contact_email, contact_phone, 
    business_description, application_status, is_deleted, created_at
)
VALUES 
    (
        'b1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333331', 
        'Sapa Discovery Travel', 'contact@sapadiscovery.vn', '0903333333', 
        'Chuyên tổ chức tour trekking chuyên nghiệp tại vùng núi Tây Bắc.', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    (
        'b2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333332', 
        'Đại Ngàn Treks', 'hello@daingan.vn', '0904444444', 
        'Đơn vị lữ hành chuyên về du lịch sinh thái và mạo hiểm.', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    (
        'b3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 
        'Cát Tiên Explorer', 'info@cattienexplorer.com', '0902222222', 
        'Đang đợi duyệt hồ sơ mở công ty du lịch.', 
        'PENDING', false, CURRENT_TIMESTAMP
    )
ON CONFLICT DO NOTHING;

-- 4. Vendor (Đối tác)
-- Ánh xạ manager_user_id từ bảng Users
INSERT INTO vendor (
    vendorid, manager_user_id, company_name, contact_email, contact_phone, 
    bank_account, bank_name, description, status, is_deleted, created_at
)
VALUES 
    (
        'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333331', 
        'Sapa Discovery Travel', 'contact@sapadiscovery.vn', '0903333333', 
        '190011112222', 'Vietcombank', 
        'Công ty du lịch hàng đầu về trải nghiệm leo núi và khám phá Tây Bắc.', 
        'ACTIVE', false, CURRENT_TIMESTAMP
    ),
    (
        'a2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333332', 
        'Đại Ngàn Treks', 'hello@daingan.vn', '0904444444', 
        '280033334444', 'Techcombank', 
        'Đưa bạn đến với những cánh rừng nguyên sinh đẹp nhất Việt Nam.', 
        'ACTIVE', false, CURRENT_TIMESTAMP
    )
ON CONFLICT (manager_user_id) DO NOTHING;

-- 5. Vendor Staff (Nhân viên Vendor)
INSERT INTO vendor_staff (
    vendor_staffid, vendor_id, user_id, is_active, is_deleted, created_at
)
VALUES 
    (
        gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 
        '44444444-4444-4444-4444-444444444441', -- Vũ Minh Em
        true, false, CURRENT_TIMESTAMP
    )
ON CONFLICT (user_id) DO NOTHING;

-- 6. Cancellation Policy (Chính sách hủy)
INSERT INTO cancellation_policy (
    policyid, vendor_id, days_before_departure, refund_percentage, 
    description, is_active, is_deleted, created_at
)
VALUES 
    -- Vendor 1 Policies
    (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 14, 100, 'Hủy trước 14 ngày, hoàn 100%', true, false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 7, 50, 'Hủy trước 7 ngày, hoàn 50%', true, false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 3, 0, 'Hủy trước 3 ngày, không hoàn tiền', true, false, CURRENT_TIMESTAMP),
    -- Vendor 2 Policies
    (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 10, 80, 'Hủy trước 10 ngày, hoàn 80%', true, false, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 5, 30, 'Hủy trước 5 ngày, hoàn 30%', true, false, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
