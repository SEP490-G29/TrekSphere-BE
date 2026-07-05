-- R__04_seed_bookings.sql
-- Seed Data for Bookings, Participants, Transactions, and Reviews

-- 1. Booking
INSERT INTO booking (
    bookingid, schedule_id, user_id, voucher_id, 
    total_price, discount_amount, refund_amount, 
    booking_status, payment_status, cancellation_reason, 
    is_deleted, created_at
)
VALUES 
    -- Booking 1: Trekker 1 đặt Tour Fansipan (Đã xác nhận & Thanh toán)
    (
        'e1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 
        '22222222-2222-2222-2222-222222222221', NULL, 
        7500000.00, 0.00, NULL, 
        'CONFIRMED', 'PAID', NULL, false, CURRENT_TIMESTAMP - interval '2 days'
    ),
    -- Booking 2: Trekker 2 đặt Tour Tà Năng (Đã hoàn thành)
    (
        'e2222222-2222-2222-2222-222222222222', 'd4444444-4444-4444-4444-444444444441', 
        '22222222-2222-2222-2222-222222222222', NULL, 
        3600000.00, 0.00, NULL, 
        'COMPLETED', 'PAID', NULL, false, CURRENT_TIMESTAMP - interval '10 days'
    ),
    -- Booking 3: Trekker 1 đặt Lảo Thẩn (Đang chờ thanh toán)
    (
        'e3333333-3333-3333-3333-333333333333', 'd2222222-2222-2222-2222-222222222221', 
        '22222222-2222-2222-2222-222222222221', NULL, 
        4200000.00, 0.00, NULL, 
        'PENDING', 'PENDING', NULL, false, CURRENT_TIMESTAMP
    ),
    -- Booking 4: Đã hủy
    (
        'e4444444-4444-4444-4444-444444444444', 'd1111111-1111-1111-1111-111111111112', 
        '22222222-2222-2222-2222-222222222222', NULL, 
        2500000.00, 0.00, 2500000.00, 
        'CANCELLED', 'REFUNDED', 'Bận việc đột xuất không thể đi', false, CURRENT_TIMESTAMP - interval '5 days'
    )
ON CONFLICT (bookingid) DO NOTHING;

-- 2. Booking Participant (Người tham gia)
INSERT INTO booking_participant (
    participantid, booking_id, full_name, date_of_birth, gender, 
    id_number, phone, email, emergency_contact, special_requirements, info_confirmed
)
VALUES 
    -- Những người tham gia cho Booking 1 (3 người)
    (gen_random_uuid(), 'e1111111-1111-1111-1111-111111111111', 'Nguyễn Văn An', '1995-05-15', 'MALE', '001095111111', '0901111111', 'trekker1@treksphere.com', '0911222333', 'Ăn chay', true),
    (gen_random_uuid(), 'e1111111-1111-1111-1111-111111111111', 'Trần Thị Mỹ', '1996-08-20', 'FEMALE', '002096222222', '0901111112', NULL, '0911222333', NULL, true),
    (gen_random_uuid(), 'e1111111-1111-1111-1111-111111111111', 'Lê Hữu Đạt', '1994-01-10', 'MALE', '003094333333', '0901111113', NULL, '0911222333', 'Dị ứng hải sản', true),
    
    -- Booking 2 (2 người)
    (gen_random_uuid(), 'e2222222-2222-2222-2222-222222222222', 'Trần Thị Bích', '1998-12-05', 'FEMALE', '004098444444', '0902222222', 'trekker2@treksphere.com', '0988777666', NULL, true),
    (gen_random_uuid(), 'e2222222-2222-2222-2222-222222222222', 'Phạm Quỳnh', '1999-03-22', 'FEMALE', '005099555555', '0902222223', NULL, '0988777666', NULL, true)
ON CONFLICT DO NOTHING;

-- 3. Transaction (Giao dịch)
INSERT INTO transaction (
    transactionid, booking_id, amount, currency, payment_gateway, 
    gateway_ref, status, paid_at, is_deleted, created_at
)
VALUES 
    (gen_random_uuid(), 'e1111111-1111-1111-1111-111111111111', 7500000.00, 'VND', 'VNPAY', 'VNP123456789', 'SUCCESS', CURRENT_TIMESTAMP - interval '2 days', false, CURRENT_TIMESTAMP - interval '2 days'),
    (gen_random_uuid(), 'e2222222-2222-2222-2222-222222222222', 3600000.00, 'VND', 'MOMO', 'MOMO98765432', 'SUCCESS', CURRENT_TIMESTAMP - interval '10 days', false, CURRENT_TIMESTAMP - interval '10 days'),
    (gen_random_uuid(), 'e4444444-4444-4444-4444-444444444444', 2500000.00, 'VND', 'ZALOPAY', 'ZLP45612378', 'REFUNDED', CURRENT_TIMESTAMP - interval '5 days', false, CURRENT_TIMESTAMP - interval '6 days')
ON CONFLICT (transactionid) DO NOTHING;

-- 4. Review (Đánh giá)
INSERT INTO review (
    reviewid, booking_id, tour_id, user_id, rating, content, 
    status, is_deleted, created_at
)
VALUES 
    -- Review cho Booking 2 (Đã hoàn thành)
    (
        gen_random_uuid(), 'e2222222-2222-2222-2222-222222222222', 'c4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 
        5, 'Tour tuyệt vời, hướng dẫn viên nhiệt tình, cảnh đồi cỏ Tà Năng vô cùng đẹp và hùng vĩ. Đồ ăn nấu trên núi cũng rất ngon.', 
        'APPROVED', false, CURRENT_TIMESTAMP - interval '2 days'
    )
ON CONFLICT (booking_id) DO NOTHING;
