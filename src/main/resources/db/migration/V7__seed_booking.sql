-- V7__seed_booking.sql
-- Seed data cho booking, booking_participant

-- 1. Bảng BOOKING
-- Booking 1: Trekker 1 đặt chuyến đi đang diễn ra (Đã thanh toán)
INSERT INTO booking (booking_id, booking_code, user_id, tour_schedule_id, voucher_id, number_of_participants, original_price, total_price, discount_amount, payment_status, booking_status, is_deleted)
SELECT gen_random_uuid(), 'BKG-0001', u.user_id, s.schedule_id, NULL, 2, s.price * 2, s.price * 2, 0, 'PAID', 'CONFIRMED', false
FROM users u, tour_schedule s
WHERE u.email = 'trekker1@treksphere.com' AND s.status = 'CLOSED';

-- Booking 2: Trekker 2 đặt chuyến đi sắp tới (Chưa thanh toán)
INSERT INTO booking (booking_id, booking_code, user_id, tour_schedule_id, voucher_id, number_of_participants, original_price, total_price, discount_amount, payment_status, booking_status, is_deleted)
SELECT gen_random_uuid(), 'BKG-0002', u.user_id, s.schedule_id, NULL, 1, s.price, s.price, 0, 'PENDING', 'PENDING', false
FROM users u, tour_schedule s
WHERE u.email = 'trekker2@treksphere.com' AND s.status = 'OPEN';

-- 2. Bảng BOOKING_PARTICIPANT
-- Người đi kèm của Booking 1
INSERT INTO booking_participant (participant_id, booking_id, full_name, phone, id_number, date_of_birth, gender, special_requirements, is_deleted)
SELECT gen_random_uuid(), b.booking_id, 'Bạn của Trekker Một', '0999111222', '012345678912', '1995-01-01', 'MALE', 'Ăn chay', false
FROM booking b
WHERE b.booking_code = 'BKG-0001';

INSERT INTO booking_participant (participant_id, booking_id, full_name, phone, id_number, date_of_birth, gender, special_requirements, is_deleted)
SELECT gen_random_uuid(), b.booking_id, 'Trekker Một (Chủ booking)', '0905000001', '987654321098', '1996-02-02', 'FEMALE', 'Bình thường', false
FROM booking b
WHERE b.booking_code = 'BKG-0001';

-- Người đi kèm của Booking 2
INSERT INTO booking_participant (participant_id, booking_id, full_name, phone, id_number, date_of_birth, gender, special_requirements, is_deleted)
SELECT gen_random_uuid(), b.booking_id, 'Trekker Hai', '0905000002', '111222333444', '1990-10-10', 'MALE', 'Bình thường', false
FROM booking b
WHERE b.booking_code = 'BKG-0002';
