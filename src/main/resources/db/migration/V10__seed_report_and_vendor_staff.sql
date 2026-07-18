-- V10__seed_report_and_vendor_staff.sql
-- Seed dữ liệu mẫu cho bảng vendor_staff và report_content

-- 1. Seed dữ liệu cho bảng vendor_staff (Gán nhân viên cho các Vendor)
-- Gán Lê Nhân Viên 1 (staff1@treksphere.com) vào Viettrekking
INSERT INTO vendor_staff (vendor_staff_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Viettrekking' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'staff1@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);

-- Gán Phạm Nhân Viên 2 (staff2@treksphere.com) vào Tổ Ong Adventure
INSERT INTO vendor_staff (vendor_staff_id, vendor_id, user_id, is_active, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT vendor_id FROM vendor WHERE company_name = 'Tổ Ong Adventure' LIMIT 1),
    (SELECT user_id FROM users WHERE email = 'staff2@treksphere.com' LIMIT 1),
    true,
    false,
    CURRENT_TIMESTAMP
);


-- 2. Seed dữ liệu cho bảng report_content (Báo cáo nội dung xấu)
-- Báo cáo Blog 'Kinh nghiệm săn mây Tà Xùa' bởi trekker2@treksphere.com
INSERT INTO report_content (report_content_id, reporter_id, blog_id, reason, status, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT user_id FROM users WHERE email = 'trekker2@treksphere.com' LIMIT 1),
    (SELECT blog_id FROM blog WHERE title = 'Kinh nghiệm săn mây Tà Xùa' LIMIT 1),
    'Nội dung chứa thông tin sai lệch, quảng cáo trá hình',
    'PENDING',
    false,
    CURRENT_TIMESTAMP
);

-- Báo cáo bình luận (blog_comment) có nội dung 'Bài viết rất hữu ích, cảm ơn bạn!' bởi trekker3@treksphere.com
INSERT INTO report_content (report_content_id, reporter_id, blog_comment_id, reason, status, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT user_id FROM users WHERE email = 'trekker3@treksphere.com' LIMIT 1),
    (SELECT blog_comment_id FROM blog_comment WHERE content = 'Bài viết rất hữu ích, cảm ơn bạn!' LIMIT 1),
    'Spam liên tục nhiều lần bình luận vô nghĩa',
    'PENDING',
    false,
    CURRENT_TIMESTAMP
);

-- Báo cáo đánh giá (review) của Booking BKG-0001 bởi trekker2@treksphere.com
INSERT INTO report_content (report_content_id, reporter_id, review_id, reason, status, is_deleted, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT user_id FROM users WHERE email = 'trekker2@treksphere.com' LIMIT 1),
    (SELECT r.review_id FROM review r JOIN booking b ON r.booking_id = b.booking_id WHERE b.booking_code = 'BKG-0001' LIMIT 1),
    'Đánh giá không khách quan, xúc phạm nhà tổ chức',
    'PENDING',
    false,
    CURRENT_TIMESTAMP
);
