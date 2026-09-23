-- ============================================================================
-- TREKKER (4)
-- ============================================================================
INSERT INTO users (
    user_id, email, full_name, password_hash, phone, date_of_birth, gender,
    avatar_url, status, email_verified, provider, bio,
    experience_level, preferred_difficulty, preferred_areas, skills,
    trust_score, trust_review_count, is_deleted, created_by
) VALUES
    ('1a2b3c4d-0001-4a1b-9c2d-000000000001', 'trekker1@treksphere.com', 'Nguyen Van An',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000001',
     DATE '1996-04-12', 'MALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/trekker1.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Mê núi phía Bắc, đã chinh phục Fansipan 3 lần. Ưu tiên cung đường vừa sức, đi chậm ngắm cảnh.',
     'INTERMEDIATE', 'MODERATE',
     '["lao cai", "ha giang", "yen bai"]'::JSONB,
     '["dinh huong ban do", "so cuu co ban", "dung leu"]'::JSONB,
     92, 7, FALSE, 'SYSTEM'),

    ('1a2b3c4d-0002-4a1b-9c2d-000000000002', 'trekker2@treksphere.com', 'Tran Thi Bich',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000002',
     DATE '1999-09-30', 'FEMALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/trekker2.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Người mới bắt đầu, thích cung ngắn 1-2 ngày và chụp ảnh.',
     'BEGINNER', 'EASY',
     '["lam dong", "khanh hoa"]'::JSONB,
     '["nhiep anh"]'::JSONB,
     100, 2, FALSE, 'SYSTEM'),

    ('1a2b3c4d-0005-4a1b-9c2d-000000000005', 'trekker3@treksphere.com', 'Pham Quoc Dat',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000005',
     DATE '1992-01-18', 'MALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/trekker3.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Trek dài ngày, mang vác nặng. Đã đi Tà Năng - Phan Dũng và Pusilung.',
     'ADVANCED', 'HARD',
     '["lai chau", "lam dong", "binh thuan"]'::JSONB,
     '["dinh huong ban do", "so cuu nang cao", "nau an da ngoai", "vuot suoi"]'::JSONB,
     88, 14, FALSE, 'SYSTEM'),

    ('1a2b3c4d-0006-4a1b-9c2d-000000000006', 'trekker4@treksphere.com', 'Do Thuy Linh',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000006',
     DATE '2001-07-05', 'FEMALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/trekker4.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Sinh viên, đi theo nhóm ghép cuối tuần.',
     'BEGINNER', 'EASY',
     '["ha giang", "cao bang"]'::JSONB,
     '[]'::JSONB,
     100, 0, FALSE, 'SYSTEM');

-- ============================================================================
-- VENDOR MANAGER (3)
-- ============================================================================
INSERT INTO users (
    user_id, email, full_name, password_hash, phone, date_of_birth, gender,
    avatar_url, status, email_verified, provider, bio,
    experience_level, preferred_difficulty, preferred_areas, skills,
    trust_score, trust_review_count, is_deleted, created_by
) VALUES
    ('1a2b3c4d-0003-4a1b-9c2d-000000000003', 'vendor1@treksphere.com', 'Le Van Cuong',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000003',
     DATE '1988-03-22', 'MALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/vendor1.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Điều hành TrekViet Adventures, 10 năm dẫn tour vùng Tây Bắc.',
     'EXPERT', 'HARD',
     '["lao cai", "lai chau", "ha giang"]'::JSONB,
     '["dan doan", "so cuu nang cao", "dinh huong ban do"]'::JSONB,
     100, 31, FALSE, 'SYSTEM'),

    ('1a2b3c4d-0004-4a1b-9c2d-000000000004', 'vendor2@treksphere.com', 'Pham Thi Dung',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000004',
     DATE '1990-11-09', 'FEMALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/vendor2.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Đồng sáng lập Mountain Trails Co, chuyên cung đường miền Trung và Tây Nguyên.',
     'EXPERT', 'MODERATE',
     '["lam dong", "khanh hoa", "quang binh"]'::JSONB,
     '["dan doan", "nau an da ngoai"]'::JSONB,
     100, 18, FALSE, 'SYSTEM'),

    ('1a2b3c4d-0007-4a1b-9c2d-000000000007', 'vendor3@treksphere.com', 'Hoang Minh Tuan',
     '$2a$12$4J3OyTUEsWYCKFGpZrW0oe1UJe6mC1RmBNrTduveEVj5/85pq7O2W', '0901000007',
     DATE '1994-06-14', 'MALE',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/avatars/vendor3.jpg',
     'ACTIVE', TRUE, 'LOCAL',
     'Hồ sơ vendor đang chờ duyệt.',
     'ADVANCED', 'MODERATE',
     '["cao bang", "bac kan"]'::JSONB,
     '["dinh huong ban do"]'::JSONB,
     100, 0, FALSE, 'SYSTEM');

-- ============================================================================
-- USER_ROLE
-- ============================================================================
INSERT INTO user_role (user_id, role_id) VALUES
    -- TREKKER
    ('1a2b3c4d-0001-4a1b-9c2d-000000000001', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0002-4a1b-9c2d-000000000002', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0005-4a1b-9c2d-000000000005', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0006-4a1b-9c2d-000000000006', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    -- Vendor manager: VENDOR
    ('1a2b3c4d-0003-4a1b-9c2d-000000000003', '6162afa8-8a00-4477-9e5e-5ce2ddb3258d'),
    ('1a2b3c4d-0004-4a1b-9c2d-000000000004', '6162afa8-8a00-4477-9e5e-5ce2ddb3258d'),
    ('1a2b3c4d-0007-4a1b-9c2d-000000000007', '6162afa8-8a00-4477-9e5e-5ce2ddb3258d'),
    -- Vendor manager: TREKKER
    ('1a2b3c4d-0003-4a1b-9c2d-000000000003', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0004-4a1b-9c2d-000000000004', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041'),
    ('1a2b3c4d-0007-4a1b-9c2d-000000000007', 'd16abaf9-b60a-4d43-bcdd-8ce760b17041');
