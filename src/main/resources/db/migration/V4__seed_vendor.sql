-- V4__seed_vendor.sql
-- Seed data cho vendor, vendor_application, vendor_staff, vendor_equipment, porter_profile, voucher

-- 1. Bảng VENDOR
INSERT INTO vendor (vendor_id, manager_id, company_name, description, contact_email, contact_phone, bank_account, bank_name, payment_qr_url, tax_code, business_license_url, status, is_deleted)
SELECT gen_random_uuid(), user_id, 'Viettrekking', 'Công ty TNHH Viettrekking - Tổ chức tour chuyên nghiệp', 'contact@viettrekking.com', '19001001', '1234567890', 'Vietcombank', 'https://example.com/qr1.png', '0101234567', 'https://example.com/license1.jpg', 'ACTIVE', false
FROM users WHERE email = 'vendor1@treksphere.com';

INSERT INTO vendor (vendor_id, manager_id, company_name, description, contact_email, contact_phone, bank_account, bank_name, payment_qr_url, tax_code, business_license_url, status, is_deleted)
SELECT gen_random_uuid(), user_id, 'Tổ Ong Adventure', 'Khám phá thiên nhiên hoang dã cùng Tổ Ong', 'hello@toong.vn', '19002002', '0987654321', 'Techcombank', 'https://example.com/qr2.png', '0207654321', 'https://example.com/license2.jpg', 'ACTIVE', false
FROM users WHERE email = 'vendor2@treksphere.com';

-- 2. Bảng VENDOR_APPLICATION (Đơn đăng ký)
INSERT INTO vendor_application (vendor_application_id, applicant_id, company_name, contact_email, contact_phone, tax_code, business_license_url, business_description, application_status, is_deleted, created_at)
SELECT gen_random_uuid(), user_id, 'Viettrekking', 'contact@viettrekking.com', '19001001', '0101234567', 'https://example.com/license1.jpg', 'Hồ sơ năng lực Viettrekking', 'APPROVED', false, CURRENT_TIMESTAMP
FROM users WHERE email = 'vendor1@treksphere.com';

INSERT INTO vendor_application (vendor_application_id, applicant_id, company_name, contact_email, contact_phone, tax_code, business_license_url, business_description, application_status, is_deleted, created_at)
SELECT gen_random_uuid(), user_id, 'Travel Me', 'hi@travelme.vn', '0999888777', '0309998887', 'https://example.com/license3.jpg', 'Đơn đăng ký mới', 'PENDING', false, CURRENT_TIMESTAMP
FROM users WHERE email = 'trekker1@treksphere.com';

-- 4. Bảng VENDOR_EQUIPMENT (Kho đồ Vendor 1)
INSERT INTO vendor_equipment (equipment_id, vendor_id, equipment_name, total_quantity)
SELECT gen_random_uuid(), vendor_id, 'Lều 4 người (NatureHike)', 20
FROM vendor WHERE company_name = 'Viettrekking';

INSERT INTO vendor_equipment (equipment_id, vendor_id, equipment_name, total_quantity)
SELECT gen_random_uuid(), vendor_id, 'Túi ngủ mùa đông (-5 độ)', 50
FROM vendor WHERE company_name = 'Viettrekking';

INSERT INTO vendor_equipment (equipment_id, vendor_id, equipment_name, total_quantity)
SELECT gen_random_uuid(), vendor_id, 'Gậy leo núi', 100
FROM vendor WHERE company_name = 'Viettrekking';

-- 5. Bảng PORTER_PROFILE (Porter của Vendor 1)
INSERT INTO porter_profile (porter_id, vendor_id, full_name, phone, gender, address, joined_date, status, is_deleted)
SELECT gen_random_uuid(), vendor_id, 'Sùng A Chứ', '0333000111', 'MALE', 'Bản Cát Cát, Sa Pa', CURRENT_DATE, 'ACTIVE', false
FROM vendor WHERE company_name = 'Viettrekking';

INSERT INTO porter_profile (porter_id, vendor_id, full_name, phone, gender, address, joined_date, status, is_deleted)
SELECT gen_random_uuid(), vendor_id, 'Giàng A Pháo', '0333000222', 'MALE', 'Tả Van, Sa Pa', CURRENT_DATE, 'ACTIVE', false
FROM vendor WHERE company_name = 'Viettrekking';

-- 6. Bảng VOUCHER (Mã giảm giá của Vendor 1)
INSERT INTO voucher (voucher_id, vendor_id, code, discount_type, discount_value, min_order_value, max_usage, used_count, valid_from, valid_until, status, is_deleted)
SELECT gen_random_uuid(), vendor_id, 'TREK2026', 'PERCENTAGE', 10, 2000000.00, 100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE', false
FROM vendor WHERE company_name = 'Viettrekking';

INSERT INTO voucher (voucher_id, vendor_id, code, discount_type, discount_value, min_order_value, max_usage, used_count, valid_from, valid_until, status, is_deleted)
SELECT gen_random_uuid(), vendor_id, 'VIP500', 'FIXED_AMOUNT', 500000.00, 5000000.00, 50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', 'ACTIVE', false
FROM vendor WHERE company_name = 'Tổ Ong Adventure';
