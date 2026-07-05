-- R__03_seed_tours.sql
-- Seed Data for Tours, Tour Images, Tour Schedules, and Vouchers

-- 1. Tour
INSERT INTO tour (
    tourid, vendor_id, created_by_user_id, tour_name, location, duration_days, 
    base_price, max_capacity, difficulty, description, highlights, includes, excludes, 
    cover_image_url, status, is_deleted, created_at
)
VALUES 
    -- Vendor 1 (Sapa Discovery Travel) Tours
    (
        'c1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333331', 
        'Trekking Chinh Phục Đỉnh Fansipan', 'Lào Cai', 2, 
        2500000.00, 15, 'HARD', 
        'Hành trình chinh phục nóc nhà Đông Dương, băng qua rừng trúc và ngắm biển mây tuyệt đẹp.', 
        'Chạm tay vào cột mốc 3143m, Ngắm biển mây hoàng hôn, Băng qua rừng trúc nguyên sinh', 
        'Xe đưa đón từ Sapa, Ăn uống các bữa trên núi, Porter mang đồ chung, Bảo hiểm du lịch', 
        'Chi phí cá nhân, Tiền tip cho porter', 
        'https://images.unsplash.com/photo-1596704381781-a90cb0667e41?w=800', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    (
        'c2222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333331', 
        'Săn Mây Đỉnh Lảo Thẩn - Y Tý', 'Lào Cai', 2, 
        2100000.00, 20, 'MODERATE', 
        'Hành trình nhẹ nhàng phù hợp cho người mới bắt đầu, ngắm biển mây Y Tý lãng mạn.', 
        'Ngắm hoàng hôn trên núi, Tiệc nướng BBQ giữa rừng, Check-in mỏm đá sống ảo', 
        'Porter, Đồ ăn tiêu chuẩn, Homestay, Vé tham quan', 
        'Đồ uống trong bữa ăn', 
        'https://images.unsplash.com/photo-1580130281320-0ef0754f2bf7?w=800', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    (
        'c3333333-3333-3333-3333-333333333333', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333331', 
        'Thử Thách Bạch Mộc Lương Tử', 'Lai Châu', 3, 
        3200000.00, 10, 'EXPERT', 
        'Cung đường trekking khó bậc nhất Tây Bắc, đi qua đỉnh Muối tuyệt đẹp.', 
        'Chinh phục đỉnh núi cao thứ 4 Việt Nam, Bình minh trên núi Muối', 
        'Đầy đủ thiết bị an toàn, Hướng dẫn viên giàu kinh nghiệm, Ăn uống, Lều trại', 
        'Vé cáp treo (nếu có), Chi tiêu cá nhân', 
        'https://images.unsplash.com/photo-1512402120009-444f6fdfaf28?w=800', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    -- Vendor 2 (Đại Ngàn Treks) Tours
    (
        'c4444444-4444-4444-4444-444444444444', 'a2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333332', 
        'Cung Đường Cỏ Pha Lê Tà Năng - Phan Dũng', 'Lâm Đồng - Bình Thuận', 2, 
        1800000.00, 25, 'MODERATE', 
        'Đi qua ba tỉnh Lâm Đồng, Ninh Thuận, Bình Thuận. Ngắm nhìn những đồi cỏ xanh ngút ngàn.', 
        'Cung trekking đẹp nhất miền Nam, Băng rừng thông, Đồi cỏ trọc', 
        'Lều trại, Các bữa ăn trong rừng, Nước suối, Y tế cơ bản', 
        'Gói vác balo cá nhân, Xe trung chuyển từ bến xe', 
        'https://images.unsplash.com/photo-1616892556555-5cc8eecf9379?w=800', 
        'APPROVED', false, CURRENT_TIMESTAMP
    ),
    (
        'c5555555-5555-5555-5555-555555555555', 'a2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333332', 
        'Khám Phá Rừng Rêu Bidoup Núi Bà', 'Lâm Đồng', 2, 
        2300000.00, 15, 'MODERATE', 
        'Trải nghiệm hệ sinh thái độc đáo tại Vườn Quốc Gia Bidoup Núi Bà với những cây thông ngàn tuổi.', 
        'Cây thông hai lá dẹt, Kéo phà qua sông Đồng Nai, Tìm hiểu văn hóa người K''Ho', 
        'Hướng dẫn viên VQG, Porter hỗ trợ, Giấy phép kiểm lâm, Lều trại', 
        'Vé xe khách đến điểm tập kết', 
        'https://images.unsplash.com/photo-1574301540306-03fcb5937a01?w=800', 
        'APPROVED', false, CURRENT_TIMESTAMP
    )
ON CONFLICT (tourid) DO NOTHING;

-- 2. Tour Image
INSERT INTO tour_image (imageid, tour_id, image_url, caption, sort_order, created_at)
VALUES 
    (gen_random_uuid(), 'c1111111-1111-1111-1111-111111111111', 'https://images.unsplash.com/photo-1596704381781-a90cb0667e41?w=800', 'Đỉnh Fansipan', 1, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'c1111111-1111-1111-1111-111111111111', 'https://images.unsplash.com/photo-1515867373801-496eb6ff0183?w=800', 'Biển mây', 2, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'https://images.unsplash.com/photo-1616892556555-5cc8eecf9379?w=800', 'Đồi cỏ Tà Năng', 1, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'https://images.unsplash.com/photo-1590494490791-c67d1ce57579?w=800', 'Rừng thông', 2, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 3. Tour Schedule
INSERT INTO tour_schedule (
    scheduleid, tour_id, departure_date, return_date, price, 
    available_slots, booked_slots, status, is_deleted, created_at
)
VALUES 
    -- Fansipan (Tour 1)
    ('d1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', CURRENT_DATE + 10, CURRENT_DATE + 11, 2500000.00, 15, 3, 'OPEN', false, CURRENT_TIMESTAMP),
    ('d1111111-1111-1111-1111-111111111112', 'c1111111-1111-1111-1111-111111111111', CURRENT_DATE + 20, CURRENT_DATE + 21, 2500000.00, 15, 0, 'OPEN', false, CURRENT_TIMESTAMP),
    
    -- Lảo Thẩn (Tour 2)
    ('d2222222-2222-2222-2222-222222222221', 'c2222222-2222-2222-2222-222222222222', CURRENT_DATE + 5, CURRENT_DATE + 6, 2100000.00, 20, 20, 'CLOSED', false, CURRENT_TIMESTAMP), -- FULL
    ('d2222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', CURRENT_DATE + 15, CURRENT_DATE + 16, 2100000.00, 20, 5, 'OPEN', false, CURRENT_TIMESTAMP),
    
    -- Tà Năng (Tour 4)
    ('d4444444-4444-4444-4444-444444444441', 'c4444444-4444-4444-4444-444444444444', CURRENT_DATE - 5, CURRENT_DATE - 4, 1800000.00, 25, 10, 'COMPLETED', false, CURRENT_TIMESTAMP), -- COMPLETED
    ('d4444444-4444-4444-4444-444444444442', 'c4444444-4444-4444-4444-444444444444', CURRENT_DATE + 12, CURRENT_DATE + 13, 1800000.00, 25, 2, 'OPEN', false, CURRENT_TIMESTAMP)
ON CONFLICT (scheduleid) DO NOTHING;

-- 4. Voucher
INSERT INTO voucher (
    voucherid, vendor_id, code, discount_type, discount_value, 
    min_order_value, max_usage, used_count, valid_from, valid_until, 
    approval_status, status, is_deleted, created_at
)
VALUES 
    (
        gen_random_uuid(), 'a1111111-1111-1111-1111-111111111111', 
        'SAPA200K', 'FIXED_AMOUNT', 200000.00, 
        2000000.00, 100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + interval '30 days', 
        'APPROVED', 'ACTIVE', false, CURRENT_TIMESTAMP
    ),
    (
        gen_random_uuid(), 'a2222222-2222-2222-2222-222222222222', 
        'DAINGAN10', 'PERCENTAGE', 10.00, 
        1000000.00, 50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + interval '15 days', 
        'APPROVED', 'ACTIVE', false, CURRENT_TIMESTAMP
    )
ON CONFLICT (code) DO NOTHING;
