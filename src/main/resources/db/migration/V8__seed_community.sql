-- V8__seed_community.sql
-- Seed data cho matching_group, matching_member, review, blog, blog_comment

-- 1. Bảng MATCHING_GROUP (Nhóm ghép đoàn)
INSERT INTO matching_group (matching_group_id, owner_id, tour_id, group_name, description, max_size, current_size, target_date, matching_deadline, status, is_deleted)
SELECT gen_random_uuid(), u.user_id, t.tour_id, 'Ghép đoàn Fansipan tháng tới', 'Mình cần tìm thêm 2 bạn leo Fan.', 4, 2, CURRENT_DATE + INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '5 days', 'OPEN', false
FROM users u, tour t
WHERE u.email = 'trekker1@treksphere.com' AND t.tour_name LIKE 'Chinh phục Fansipan%';

-- 2. Bảng MATCHING_MEMBER (Thành viên nhóm ghép)
-- Trekker 1 (Owner)
INSERT INTO matching_member (matching_member_id, group_id, user_id, role, status, is_deleted)
SELECT gen_random_uuid(), m.matching_group_id, u.user_id, 'OWNER', 'APPROVED', false
FROM matching_group m, users u
WHERE m.group_name = 'Ghép đoàn Fansipan tháng tới' AND u.email = 'trekker1@treksphere.com';

-- Trekker 2 (Đã duyệt)
INSERT INTO matching_member (matching_member_id, group_id, user_id, role, status, is_deleted)
SELECT gen_random_uuid(), m.matching_group_id, u.user_id, 'MEMBER', 'APPROVED', false
FROM matching_group m, users u
WHERE m.group_name = 'Ghép đoàn Fansipan tháng tới' AND u.email = 'trekker2@treksphere.com';

-- Trekker 3 (Chờ duyệt)
INSERT INTO matching_member (matching_member_id, group_id, user_id, role, status, is_deleted)
SELECT gen_random_uuid(), m.matching_group_id, u.user_id, 'MEMBER', 'PENDING', false
FROM matching_group m, users u
WHERE m.group_name = 'Ghép đoàn Fansipan tháng tới' AND u.email = 'trekker3@treksphere.com';

-- 3. Bảng REVIEW (Đánh giá)
-- Trekker 1 đánh giá Booking 1
INSERT INTO review (review_id, tour_id, user_id, booking_id, rating, content, status, is_deleted)
SELECT gen_random_uuid(), t.tour_id, u.user_id, b.booking_id, 5, 'Tour tổ chức rất tốt, HDV nhiệt tình.', 'APPROVED', false
FROM booking b
JOIN users u ON b.user_id = u.user_id
JOIN tour_schedule s ON b.tour_schedule_id = s.schedule_id
JOIN tour t ON s.tour_id = t.tour_id
WHERE u.email = 'trekker1@treksphere.com' AND b.booking_code = 'BKG-0001';

-- 4. Bảng BLOG (Bài viết)
INSERT INTO blog (blog_id, user_id, title, content, cover_image_url, view_count, status, is_deleted)
SELECT gen_random_uuid(), user_id, 'Kinh nghiệm săn mây Tà Xùa', 'Đây là một số mẹo nhỏ khi đi Tà Xùa...', 'https://example.com/blog1.jpg', 150, 'PUBLISHED', false
FROM users WHERE email = 'trekker1@treksphere.com';

-- 5. Bảng BLOG_COMMENT (Bình luận blog)
INSERT INTO blog_comment (blog_comment_id, blog_id, user_id, parent_comment_id, content, status, is_deleted)
SELECT gen_random_uuid(), b.blog_id, u.user_id, NULL, 'Bài viết rất hữu ích, cảm ơn bạn!', 'APPROVED', false
FROM blog b, users u
WHERE b.title = 'Kinh nghiệm săn mây Tà Xùa' AND u.email = 'trekker2@treksphere.com';
