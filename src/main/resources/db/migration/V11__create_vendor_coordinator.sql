-- V11__create_vendor_coordinator.sql
-- Tạo bảng vendor_coordinator và seed dữ liệu mẫu cho Hướng dẫn viên trực thuộc Vendor

-- 1. Tạo bảng vendor_coordinator
CREATE TABLE vendor_coordinator (
    vendor_coordinator_id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_by VARCHAR(255),
    
    CONSTRAINT fk_vc_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id),
    CONSTRAINT fk_vc_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uq_vc_user UNIQUE (user_id) -- Mỗi Coordinator chỉ thuộc tối đa 1 Vendor
);

-- 2. Seed dữ liệu mẫu cho bảng vendor_coordinator
-- Gán Hoàng Điều Phối 1 (coordinator1@treksphere.com) vào Viettrekking
INSERT INTO vendor_coordinator (vendor_coordinator_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Viettrekking' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'coordinator1@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);

-- Gán Bùi Điều Phối 2 (coordinator2@treksphere.com) vào Tổ Ong Adventure
INSERT INTO vendor_coordinator (vendor_coordinator_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Tổ Ong Adventure' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'coordinator2@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);
