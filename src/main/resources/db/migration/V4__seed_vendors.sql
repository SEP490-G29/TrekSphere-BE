-- ============================================================================
-- VENDOR (3): 2 ACTIVE đang bán tour, 1 PENDING chờ kích hoạt
-- ============================================================================
INSERT INTO vendor (
    vendor_id, manager_id, company_name, description, logo_url,
    contact_email, contact_phone, tax_code, business_license_url,
    business_address, legal_representative_name, legal_representative_position,
    website_url, status, is_deleted, created_by
) VALUES
    ('2b3c4d5e-0001-4b2c-8d3e-000000000001', '1a2b3c4d-0003-4a1b-9c2d-000000000003',
     'TrekViet Adventures',
     'Đơn vị tổ chức trekking vùng núi Tây Bắc từ 2015. Chuyên Fansipan, Lảo Thẩn, Bạch Mộc Lương Tử.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/logos/trekviet.png',
     'contact@trekviet.example.com', '0281000001', 'TAXSAMPLE0001',
     'https://example.com/licenses/trekviet.pdf',
     'So 12 Ngo 45 Duong Cau Giay, Quan Cau Giay, Ha Noi',
     'Le Van Cuong', 'Giam doc',
     'https://trekviet.example.com',
     'ACTIVE', FALSE, 'SYSTEM'),

    ('2b3c4d5e-0002-4b2c-8d3e-000000000002', '1a2b3c4d-0004-4a1b-9c2d-000000000004',
     'Mountain Trails Co',
     'Tổ chức tour trekking và cắm trại khu vực miền Trung - Tây Nguyên, đội ngũ porter bản địa.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/logos/mountaintrails.png',
     'contact@mountaintrails.example.com', '0281000002', 'TAXSAMPLE0002',
     'https://example.com/licenses/mountaintrails.pdf',
     'So 88 Duong Tran Phu, Phuong 4, Da Lat, Lam Dong',
     'Pham Thi Dung', 'Tong giam doc',
     'https://mountaintrails.example.com',
     'ACTIVE', FALSE, 'SYSTEM'),

    ('2b3c4d5e-0003-4b2c-8d3e-000000000003', '1a2b3c4d-0007-4a1b-9c2d-000000000007',
     'Non Nuoc Cao Bang Trek',
     'Đơn vị mới, tập trung cung đường Cao Bằng - Bắc Kạn. Đang chờ kích hoạt.',
     NULL,
     'contact@nonnuoccb.example.com', '0281000003', 'TAXSAMPLE0003',
     'https://example.com/licenses/nonnuoccb.pdf',
     'So 5 Duong Kim Dong, Phuong Hop Giang, Cao Bang',
     'Hoang Minh Tuan', 'Giam doc',
     NULL,
     'PENDING', FALSE, 'SYSTEM');

-- ============================================================================
-- VENDOR_APPLICATION (4)
-- ============================================================================
INSERT INTO vendor_application (
    vendor_application_id, applicant_id, company_name, contact_email, contact_phone,
    tax_code, business_license_url, business_description, application_status,
    rejection_reason, business_address, legal_representative_name,
    legal_representative_position, website_url, reviewed_by, reviewed_at,
    vendor_id, is_deleted, created_by
) VALUES
    ('7a8b9c0d-0001-4e5f-9a1b-000000000001', '1a2b3c4d-0003-4a1b-9c2d-000000000003',
     'TrekViet Adventures', 'contact@trekviet.example.com', '0281000001',
     'TAXSAMPLE0001', 'https://example.com/licenses/trekviet.pdf',
     'Cong ty to chuc tour trekking vung nui phia Bac, thanh lap 2015, doi ngu 12 huong dan vien.',
     'APPROVED', NULL,
     'So 12 Ngo 45 Duong Cau Giay, Quan Cau Giay, Ha Noi',
     'Le Van Cuong', 'Giam doc', 'https://trekviet.example.com',
     'b70fbe4a-6199-416a-aae1-536b8c497b3e', CURRENT_TIMESTAMP - INTERVAL '120 days',
     '2b3c4d5e-0001-4b2c-8d3e-000000000001', FALSE, 'SYSTEM'),

    ('7a8b9c0d-0002-4e5f-9a1b-000000000002', '1a2b3c4d-0004-4a1b-9c2d-000000000004',
     'Mountain Trails Co', 'contact@mountaintrails.example.com', '0281000002',
     'TAXSAMPLE0002', 'https://example.com/licenses/mountaintrails.pdf',
     'Cong ty to chuc tour trekking va cam trai khu vuc mien Trung - Tay Nguyen.',
     'APPROVED', NULL,
     'So 88 Duong Tran Phu, Phuong 4, Da Lat, Lam Dong',
     'Pham Thi Dung', 'Tong giam doc', 'https://mountaintrails.example.com',
     'b70fbe4a-6199-416a-aae1-536b8c497b3e', CURRENT_TIMESTAMP - INTERVAL '95 days',
     '2b3c4d5e-0002-4b2c-8d3e-000000000002', FALSE, 'SYSTEM'),

    ('7a8b9c0d-0003-4e5f-9a1b-000000000003', '1a2b3c4d-0007-4a1b-9c2d-000000000007',
     'Non Nuoc Cao Bang Trek', 'contact@nonnuoccb.example.com', '0281000003',
     'TAXSAMPLE0003', 'https://example.com/licenses/nonnuoccb.pdf',
     'Don vi moi thanh lap, tap trung cung duong Cao Bang - Bac Kan.',
     'PENDING', NULL,
     'So 5 Duong Kim Dong, Phuong Hop Giang, Cao Bang',
     'Hoang Minh Tuan', 'Giam doc', NULL,
     NULL, NULL,
     NULL, FALSE, 'SYSTEM'),

    ('7a8b9c0d-0004-4e5f-9a1b-000000000004', '1a2b3c4d-0005-4a1b-9c2d-000000000005',
     'Dat Trek Services', NULL, NULL,
     NULL, NULL, NULL,
     'DRAFT', NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL,
     NULL, FALSE, 'SYSTEM');
