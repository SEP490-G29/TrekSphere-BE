-- R__06_seed_communication.sql
-- Seed Data for Conversations, Messages, Notifications, and Reports

-- 1. Conversation (Cuộc trò chuyện)
INSERT INTO conversation (
    conversationid, conversation_type, title, 
    status, is_deleted, created_at
)
VALUES 
    (
        '77777777-1111-1111-1111-111111111111', 'MATCHING_GROUP', 'Nhóm leo Bạch Mộc Lương Tử cuối tháng 8', 
        'ACTIVE', false, CURRENT_TIMESTAMP
    ),
    (
        '77777777-2222-2222-2222-222222222222', 'DIRECT', 'Hỗ trợ khách hàng - An & Vendor Sapa', 
        'ACTIVE', false, CURRENT_TIMESTAMP - interval '1 day'
    )
ON CONFLICT (conversationid) DO NOTHING;

-- 2. Conversation Participant (Người tham gia Chat)
INSERT INTO conversation_participant (conversation_id, user_id)
VALUES 
    -- Group Chat Bạch Mộc
    ('77777777-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221'),
    ('77777777-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222'),
    -- Direct Chat
    ('77777777-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221'), -- Trekker An
    ('77777777-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333331')  -- Vendor Cường
ON CONFLICT (conversation_id, user_id) DO NOTHING;

-- 3. Message (Tin nhắn)
INSERT INTO message (
    messageid, conversation_id, sender_user_id, message_type, 
    content, is_read, is_deleted, created_at
)
VALUES 
    -- Trong Group Bạch Mộc
    (
        gen_random_uuid(), '77777777-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 'TEXT', 
        'Chào mọi người, mình mới lập nhóm, ai muốn đi Bạch Mộc thì join nhé!', 
        true, false, CURRENT_TIMESTAMP - interval '1 hour'
    ),
    -- Direct Chat
    (
        gen_random_uuid(), '77777777-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221', 'TEXT', 
        'Chào admin, cho mình hỏi tour Fansipan cuối tháng này còn chỗ không ạ?', 
        true, false, CURRENT_TIMESTAMP - interval '1 day'
    ),
    (
        gen_random_uuid(), '77777777-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333331', 'TEXT', 
        'Chào bạn An, tour Fansipan hiện tại vẫn còn 5 slot nhé, bạn có thể đặt trên hệ thống.', 
        false, false, CURRENT_TIMESTAMP - interval '23 hours'
    )
ON CONFLICT (messageid) DO NOTHING;

-- 4. Notification (Thông báo)
INSERT INTO notification (
    notificationid, user_id, event_type, reference_type, referenceid, 
    title, message, is_read, is_deleted, created_at
)
VALUES 
    (
        gen_random_uuid(), '22222222-2222-2222-2222-222222222221', 
        'BOOKING_CONFIRMED', 'BOOKING', 'e1111111-1111-1111-1111-111111111111', 
        'Xác nhận đặt tour thành công', 
        'Tour Fansipan của bạn đã được xác nhận. Vui lòng chuẩn bị sức khỏe tốt!', 
        true, false, CURRENT_TIMESTAMP - interval '1 day'
    ),
    (
        gen_random_uuid(), '33333333-3333-3333-3333-333333333331', 
        'PAYMENT_SUCCESS', 'BOOKING', 'e1111111-1111-1111-1111-111111111111', 
        'Khách hàng đã thanh toán', 
        'Trekker Nguyễn Văn An đã thanh toán thành công 7,500,000 VND cho tour Fansipan.', 
        false, false, CURRENT_TIMESTAMP - interval '2 days'
    )
ON CONFLICT (notificationid) DO NOTHING;

-- 5. Report Content (Báo cáo vi phạm)
INSERT INTO report_content (
    reportid, reporter_user_id, target_type, targetid, 
    reason, detail, status, reported_at
)
VALUES 
    (
        gen_random_uuid(), '22222222-2222-2222-2222-222222222221', 
        'COMMENT', 'b1a11111-1111-1111-1111-111111111111', -- Giả sử targetid là ID của comment (hoặc blog giả lập)
        'Spam hoặc quảng cáo', 
        'Comment này chứa link dẫn tới trang web cá cược.', 
        'PENDING', CURRENT_TIMESTAMP - interval '2 hours'
    )
ON CONFLICT (reportid) DO NOTHING;
