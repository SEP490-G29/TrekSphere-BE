-- V9__seed_chat_notification.sql
-- Seed data cho conversation, conversation_participant, message, notification

-- 1. Bảng CONVERSATION (Cuộc hội thoại)
INSERT INTO conversation (conversation_id, title, conversation_type, last_message_at, is_deleted)
VALUES (gen_random_uuid(), NULL, 'PRIVATE', CURRENT_TIMESTAMP, false);

-- 2. Bảng CONVERSATION_PARTICIPANT (Người tham gia chat)
INSERT INTO conversation_participant (conversation_id, user_id)
SELECT c.conversation_id, u.user_id
FROM conversation c, users u
WHERE c.conversation_type = 'PRIVATE' AND u.email = 'trekker1@treksphere.com';

INSERT INTO conversation_participant (conversation_id, user_id)
SELECT c.conversation_id, u.user_id
FROM conversation c, users u
WHERE c.conversation_type = 'PRIVATE' AND u.email = 'trekker2@treksphere.com';

-- 3. Bảng MESSAGE (Tin nhắn)
INSERT INTO message (message_id, conversation_id, sender_id, content, is_read, is_deleted)
SELECT gen_random_uuid(), c.conversation_id, u.user_id, 'Chào bạn, cho mình hỏi về Tà Xùa nhé', true, false
FROM conversation c, users u
WHERE c.conversation_type = 'PRIVATE' AND u.email = 'trekker1@treksphere.com';

INSERT INTO message (message_id, conversation_id, sender_id, content, is_read, is_deleted)
SELECT gen_random_uuid(), c.conversation_id, u.user_id, 'Chào bạn, bạn định đi khi nào?', false, false
FROM conversation c, users u
WHERE c.conversation_type = 'PRIVATE' AND u.email = 'trekker2@treksphere.com';

-- 4. Bảng NOTIFICATION (Thông báo)
INSERT INTO notification (notification_id, recipient_id, title, content, is_read, event_type, reference_type, reference_id, is_deleted)
SELECT gen_random_uuid(), user_id, 'Đặt tour thành công', 'Cảm ơn bạn đã đặt tour Fansipan.', false, 'BOOKING_SUCCESS', 'BOOKING', NULL, false
FROM users WHERE email = 'trekker1@treksphere.com';

INSERT INTO notification (notification_id, recipient_id, title, content, is_read, event_type, reference_type, reference_id, is_deleted)
SELECT gen_random_uuid(), user_id, 'Duyệt tham gia nhóm', 'Yêu cầu tham gia nhóm ghép đoàn của bạn đang được duyệt.', false, 'MATCHING_PENDING', 'MATCHING_GROUP', NULL, false
FROM users WHERE email = 'trekker3@treksphere.com';
