-- V5__seed_tour.sql
-- Seed data cho cancellation_policy, tour, tour_image, tour_schedule, tour_checkpoint

-- 1. Bảng CANCELLATION_POLICY (Chính sách hủy)
INSERT INTO cancellation_policy (cancellation_policy_id, vendor_id, cancel_before_days, refund_percentage, description, is_active)
SELECT gen_random_uuid(), vendor_id, 7, 100, 'Hủy trước 7 ngày hoàn 100%', true
FROM vendor WHERE company_name = 'Viettrekking';

INSERT INTO cancellation_policy (cancellation_policy_id, vendor_id, cancel_before_days, refund_percentage, description, is_active)
SELECT gen_random_uuid(), vendor_id, 3, 50, 'Hủy trước 3 ngày hoàn 50%', true
FROM vendor WHERE company_name = 'Viettrekking';

-- 2. Bảng TOUR
INSERT INTO tour (tour_id, vendor_id, tour_name, description, duration_days, base_price, min_capacity, max_capacity, total_distance_km, difficulty, status, cover_image_url, location, highlights, includes, excludes, creator_id, is_deleted, created_at)
SELECT gen_random_uuid(), v.vendor_id, 'Chinh phục Fansipan - Nóc nhà Đông Dương', 'Hành trình 2 ngày 1 đêm chinh phục đỉnh Fansipan hùng vĩ.', 2, 2500000.00, 5, 20, 15.5, 'HARD', 'PUBLISHED', 'https://example.com/fansi.jpg', 'Sa Pa, Lào Cai', 'Đỉnh Fansipan, Rừng Hoàng Liên Sơn, Biển mây', 'Xe giường nằm, Hướng dẫn viên, 3 bữa ăn', 'VAT, Chi phí cá nhân', u.user_id, false, CURRENT_TIMESTAMP
FROM vendor v, users u
WHERE v.company_name = 'Viettrekking' AND u.email = 'vendor1@treksphere.com';

INSERT INTO tour (tour_id, vendor_id, tour_name, description, duration_days, base_price, min_capacity, max_capacity, total_distance_km, difficulty, status, cover_image_url, location, highlights, includes, excludes, creator_id, is_deleted, created_at)
SELECT gen_random_uuid(), v.vendor_id, 'Săn mây Tà Xùa', 'Trải nghiệm săn mây tại thiên đường Tà Xùa.', 2, 1800000.00, 5, 15, 10.0, 'EASY', 'PUBLISHED', 'https://example.com/taxua.jpg', 'Bắc Yên, Sơn La', 'Sống lưng khủng long, Đỉnh gió', 'Xe đưa đón, Homestay', 'Chi phí cá nhân', u.user_id, false, CURRENT_TIMESTAMP
FROM vendor v, users u
WHERE v.company_name = 'Viettrekking' AND u.email = 'vendor1@treksphere.com';

INSERT INTO tour (tour_id, vendor_id, tour_name, description, duration_days, base_price, min_capacity, max_capacity, total_distance_km, difficulty, status, cover_image_url, location, highlights, includes, excludes, creator_id, is_deleted, created_at)
SELECT gen_random_uuid(), v.vendor_id, 'Lảo Thẩn - Nơi mặt trời thức giấc', 'Chinh phục Lảo Thẩn 2 ngày 1 đêm.', 2, 2200000.00, 5, 25, 14.0, 'MODERATE', 'PUBLISHED', 'https://example.com/laothan.jpg', 'Y Tý, Lào Cai', 'Đỉnh Lảo Thẩn, Cây cô đơn', 'Porter, Lều trại', 'Túi ngủ cá nhân', u.user_id, false, CURRENT_TIMESTAMP
FROM vendor v, users u
WHERE v.company_name = 'Tổ Ong Adventure' AND u.email = 'vendor2@treksphere.com';

-- 3. Bảng TOUR_IMAGE
INSERT INTO tour_image (image_id, tour_id, image_url, caption, sort_order)
SELECT gen_random_uuid(), tour_id, 'https://example.com/fansi1.jpg', 'Biển mây Fansipan', 1
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

INSERT INTO tour_image (image_id, tour_id, image_url, caption, sort_order)
SELECT gen_random_uuid(), tour_id, 'https://example.com/fansi2.jpg', 'Cột mốc 3143m', 2
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

-- 4. Bảng TOUR_SCHEDULE (Lịch trình tổ chức tour)
INSERT INTO tour_schedule (schedule_id, tour_id, departure_date, return_date, price, booked_slots, available_slots, status)
SELECT gen_random_uuid(), tour_id, CURRENT_DATE + INTERVAL '10 days', CURRENT_DATE + INTERVAL '12 days', base_price, 0, max_capacity, 'OPEN'
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

INSERT INTO tour_schedule (schedule_id, tour_id, departure_date, return_date, price, booked_slots, available_slots, status)
SELECT gen_random_uuid(), tour_id, CURRENT_DATE - INTERVAL '1 days', CURRENT_DATE + INTERVAL '1 days', base_price, 5, max_capacity - 5, 'CLOSED'
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

INSERT INTO tour_schedule (schedule_id, tour_id, departure_date, return_date, price, booked_slots, available_slots, status)
SELECT gen_random_uuid(), tour_id, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '28 days', base_price, 10, max_capacity - 10, 'COMPLETED'
FROM tour WHERE tour_name LIKE 'Săn mây Tà Xùa%';

-- 5. Bảng TOUR_CHECKPOINT (Các điểm dừng chân/Check-in)
INSERT INTO tour_checkpoint (checkpoint_id, tour_id, checkpoint_name, description, latitude, longitude, altitude, checkpoint_order, checkpoint_image_url)
SELECT gen_random_uuid(), tour_id, 'Trạm Tôn', 'Điểm xuất phát', 22.3551, 103.7744, 1900.0, 1, 'https://example.com/tramton.jpg'
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

INSERT INTO tour_checkpoint (checkpoint_id, tour_id, checkpoint_name, description, latitude, longitude, altitude, checkpoint_order, checkpoint_image_url)
SELECT gen_random_uuid(), tour_id, 'Lán 2800m', 'Điểm nghỉ đêm', 22.3168, 103.7770, 2800.0, 2, 'https://example.com/lan2800.jpg'
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';

INSERT INTO tour_checkpoint (checkpoint_id, tour_id, checkpoint_name, description, latitude, longitude, altitude, checkpoint_order, checkpoint_image_url)
SELECT gen_random_uuid(), tour_id, 'Đỉnh Fansipan', 'Cột mốc 3143m', 22.3039, 103.7753, 3143.0, 3, 'https://example.com/dinhfansi.jpg'
FROM tour WHERE tour_name LIKE 'Chinh phục Fansipan%';
