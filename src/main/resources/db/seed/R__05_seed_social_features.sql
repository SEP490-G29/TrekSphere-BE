-- R__05_seed_social_features.sql
-- Seed Data for Blogs, Comments, Matching Groups, and Matching Members

-- 1. Blog
INSERT INTO blog (
    blogid, user_id, title, content, cover_image_url, 
    status, view_count, is_deleted, created_at
)
VALUES 
    (
        'b1a11111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 
        'Kinh nghiệm lần đầu leo Fansipan', 
        '<p>Leo Fansipan không hề dễ dàng như mình nghĩ, nhưng cảm giác đứng trên đỉnh núi thật tuyệt vời.</p><p>Mình khuyên các bạn nên tập thể lực trước ít nhất 1 tháng.</p>', 
        'https://images.unsplash.com/photo-1596704381781-a90cb0667e41?w=800', 
        'PUBLISHED', 150, false, CURRENT_TIMESTAMP - interval '20 days'
    ),
    (
        'b1a22222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 
        'Vẻ đẹp mùa cỏ cháy Tà Năng - Phan Dũng', 
        '<p>Tà Năng mùa cỏ cháy mang một vẻ đẹp hoang sơ và lãng mạn vô cùng. Đi dạo trên sống lưng khủng long, ngắm nhìn hoàng hôn là trải nghiệm không thể quên.</p>', 
        'https://images.unsplash.com/photo-1616892556555-5cc8eecf9379?w=800', 
        'PUBLISHED', 342, false, CURRENT_TIMESTAMP - interval '5 days'
    )
ON CONFLICT (blogid) DO NOTHING;

-- 2. Blog Comment
INSERT INTO blog_comment (
    commentid, blog_id, parent_comment_id, user_id, 
    content, status, is_deleted, created_at
)
VALUES 
    (
        gen_random_uuid(), 'b1a11111-1111-1111-1111-111111111111', NULL, '22222222-2222-2222-2222-222222222222', 
        'Bài viết rất hay, cảm ơn bạn đã chia sẻ!', 'ACTIVE', false, CURRENT_TIMESTAMP - interval '19 days'
    ),
    (
        gen_random_uuid(), 'b1a22222-2222-2222-2222-222222222222', NULL, '22222222-2222-2222-2222-222222222221', 
        'Đẹp quá, năm sau nhất định mình sẽ đi Tà Năng.', 'ACTIVE', false, CURRENT_TIMESTAMP - interval '4 days'
    )
ON CONFLICT (commentid) DO NOTHING;

-- 3. Matching Group (Nhóm tìm bạn đồng hành)
INSERT INTO matching_group (
    groupid, owner_user_id, tour_id, group_name, description, 
    target_date, max_size, current_size, status, is_deleted, created_at
)
VALUES 
    (
        'f1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 'c3333333-3333-3333-3333-333333333333', 
        'Nhóm leo Bạch Mộc Lương Tử cuối tháng 8', 
        'Mình đang tìm 4-5 bạn đồng hành để lập nhóm đi Bạch Mộc. Yêu cầu thể lực tốt.', 
        CURRENT_DATE + 30, 6, 2, 'OPEN', false, CURRENT_TIMESTAMP
    ),
    (
        'f2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', 
        'Team săn mây Lảo Thẩn', 
        'Rủ rê anh chị em đi Lảo Thẩn ngắm mây, ưu tiên các bạn nữ cho dễ nói chuyện.', 
        CURRENT_DATE + 15, 10, 10, 'FULL', false, CURRENT_TIMESTAMP - interval '2 days'
    )
ON CONFLICT (groupid) DO NOTHING;

-- 4. Matching Member (Thành viên nhóm)
INSERT INTO matching_member (
    memberid, group_id, user_id, role, join_status, joined_at
)
VALUES 
    -- Group 1 (Bạch Mộc)
    (
        gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 
        'OWNER', 'ACCEPTED', CURRENT_TIMESTAMP
    ),
    (
        gen_random_uuid(), 'f1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 
        'MEMBER', 'PENDING', CURRENT_TIMESTAMP
    ),
    -- Group 2 (Lảo Thẩn - Full)
    (
        gen_random_uuid(), 'f2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 
        'OWNER', 'ACCEPTED', CURRENT_TIMESTAMP - interval '2 days'
    )
ON CONFLICT (group_id, user_id) DO NOTHING;
