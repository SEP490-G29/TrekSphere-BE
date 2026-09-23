-- ============================================================================
-- BLOG
-- ============================================================================
INSERT INTO blog (
    blog_id, user_id, title, content, cover_image_url, view_count, status,
    is_deleted, created_by, created_at
) VALUES
    ('9d0e1f2a-0001-4d9e-8f10-000000000001', '1a2b3c4d-0001-4a1b-9c2d-000000000001',
     'Kinh nghiệm leo Fansipan lần đầu: 7 điều tôi ước mình biết sớm hơn',
     E'Lần đầu leo Fansipan tôi mắc gần như đủ mọi lỗi cơ bản.\n\n**1. Giày.** Đừng dùng giày thể thao đế mềm. Đoạn từ 2200m trở lên đá ướt quanh năm, tôi trượt ngã bốn lần trong một buổi sáng.\n\n**2. Nước.** Mang 2 lít là đủ, đừng mang 4 lít. Dọc đường có ba điểm tiếp nước sạch.\n\n**3. Tốc độ.** Đi chậm hơn bạn nghĩ. Nhóm tôi cố bám theo porter và kiệt sức ngay ở ngày đầu.\n\n**4. Đồ giữ ấm.** Đêm ở 2800m xuống dưới 8 độ. Một chiếc áo phao mỏng là không đủ.\n\n**5. Đèn đội đầu.** Bắt buộc. Hôm đó chúng tôi xuất phát lúc 4h sáng để kịp bình minh.\n\n**6. Gậy trek.** Tiết kiệm đầu gối rất nhiều ở chiều xuống.\n\n**7. Thể lực.** Tập leo cầu thang bộ ít nhất 3 tuần trước chuyến đi.\n\nTổng kết: đây là cung đẹp, nhưng không nên xem nhẹ.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/fansipan-kinhnghiem.jpg',
     1284, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '42 days'),

    ('9d0e1f2a-0002-4d9e-8f10-000000000002', '1a2b3c4d-0005-4a1b-9c2d-000000000005',
     'Tà Năng - Phan Dũng mùa khô: checklist đồ mang theo',
     E'Đi cung này bốn lần rồi, đây là danh sách tôi chốt lại.\n\n## Bắt buộc\n- Balo 40-45L có đai hông\n- Giày trek cổ cao đã đi rão ít nhất 2 tuần\n- Bình nước 2L + viên lọc nước\n- Đèn đội đầu và pin dự phòng\n- Áo mưa bộ, không dùng áo mưa giấy\n\n## Nên có\n- Gậy trek: đoạn xuống dốc ngày 3 rất dài\n- Muối khoáng hoặc viên sủi điện giải\n- Băng gạc và thuốc chống côn trùng\n\n## Đừng mang\n- Loa bluetooth\n- Quá nhiều đồ hộp, nặng mà ít calo\n\nMùa khô từ tháng 11 tới tháng 4 là đẹp nhất. Mùa mưa suối Yavly dâng nhanh, không nên tự đi.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/tanang-checklist.jpg',
     957, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '35 days'),

    ('9d0e1f2a-0003-4d9e-8f10-000000000003', '1a2b3c4d-0003-4a1b-9c2d-000000000003',
     'Tại sao chúng tôi luôn bố trí 1 porter cho mỗi 3 khách',
     E'Nhiều người hỏi vì sao tour của TrekViet đắt hơn mặt bằng chung khoảng 15%.\n\nCâu trả lời nằm ở tỉ lệ porter. Chúng tôi giữ tỉ lệ 1:3 thay vì 1:6 như thông lệ.\n\nLý do rất thực tế: khi có sự cố ở độ cao trên 2500m, một porter phải ở lại với người gặp vấn đề trong khi nhóm vẫn tiếp tục. Với tỉ lệ 1:6, việc tách nhóm đồng nghĩa phần còn lại không còn người dẫn.\n\nChúng tôi cũng trả công porter theo ngày công thực tế, không khoán trọn gói. Điều này khiến giá tour cao hơn nhưng giữ được đội porter ổn định qua nhiều mùa.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/porter-ratio.jpg',
     612, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '28 days'),

    ('9d0e1f2a-0004-4d9e-8f10-000000000004', '1a2b3c4d-0002-4a1b-9c2d-000000000002',
     'Langbiang trong ngày - cung nhập môn cho người sợ trek',
     E'Tôi từng nghĩ trek là thứ gì đó rất xa vời cho tới khi đi Langbiang.\n\nCung này chỉ 7km cả đi lẫn về, dốc thoải, có đường mòn rõ ràng và sóng điện thoại gần như toàn tuyến. Mất khoảng 4 tiếng kể cả nghỉ.\n\nĐiều tôi thích nhất là bạn được cảm giác "lên đỉnh" thật sự mà không cần chuẩn bị gì nhiều. Nếu bạn đang phân vân có nên thử trek hay không, hãy bắt đầu từ đây thay vì lao thẳng vào Fansipan như nhiều người.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/langbiang-nhapmon.jpg',
     1893, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '24 days'),

    ('9d0e1f2a-0005-4d9e-8f10-000000000005', '1a2b3c4d-0004-4a1b-9c2d-000000000004',
     'Ngủ một đêm trong Hang Én: những gì brochure không nói',
     E'Hang Én đẹp đúng như ảnh. Nhưng có vài thứ nên biết trước.\n\nThứ nhất, bạn sẽ lội nước nhiều hơn tưởng tượng. Đoạn từ bản Đoòng vào hang phải qua suối Rào Thương khoảng chục lần.\n\nThứ hai, trong hang lạnh và ẩm. Túi ngủ được cấp là đủ, nhưng quần áo ướt thì nên thay hết trước khi ngủ.\n\nThứ ba, chim én thật sự rất ồn vào sáng sớm. Đó cũng là lý do hang có tên như vậy.\n\nVà thứ tư, tín hiệu điện thoại bằng không suốt 2 ngày. Với nhiều người đó mới là phần giá trị nhất của chuyến đi.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/hangen-mot-dem.jpg',
     2450, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '20 days'),

    ('9d0e1f2a-0006-4d9e-8f10-000000000006', '1a2b3c4d-0006-4a1b-9c2d-000000000006',
     'Đi trek với nhóm ghép: làm sao để không gặp người lệch tông',
     E'Mình là sinh viên, không có nhóm bạn nào cùng thích trek nên toàn đi ghép.\n\nSau vài chuyến, mình rút ra mấy điều:\n\n- Hỏi rõ tốc độ dự kiến trước khi tham gia. "Đi chill" với mỗi người là một tốc độ khác nhau.\n- Xem hồ sơ và điểm uy tín của chủ nhóm.\n- Thống nhất chi phí chung ngay từ đầu, đừng để tới cuối chuyến mới chia.\n- Nói trước nếu bạn đi chậm. Không ai khó chịu vì điều đó, người ta chỉ khó chịu khi bị bất ngờ.\n\nChuyến gần nhất mình đi Tà Xùa với nhóm 8 người lạ và mọi thứ rất ổn.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/nhom-ghep.jpg',
     734, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '16 days'),

    ('9d0e1f2a-0007-4d9e-8f10-000000000007', '1a2b3c4d-0001-4a1b-9c2d-000000000001',
     'Săn mây Lảo Thẩn: chọn đúng thời điểm quan trọng hơn may mắn',
     E'Biển mây không phải chuyện hên xui hoàn toàn.\n\nĐiều kiện lý tưởng: sau một đợt mưa nhẹ, trời hửng, độ ẩm cao và không có gió mạnh. Thường rơi vào tháng 10 tới tháng 3.\n\nMình theo dõi dự báo 3 ngày trước khi đi, nhìn vào hai chỉ số: độ ẩm trên 85% và tốc độ gió dưới 15km/h. Bốn lần áp dụng thì trúng ba.\n\nLảo Thẩn dễ đi hơn Bạch Mộc nhiều, phù hợp nếu bạn muốn ưu tiên khả năng săn được mây thay vì độ khó.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/laothan-sanmay.jpg',
     1102, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '12 days'),

    ('9d0e1f2a-0008-4d9e-8f10-000000000008', '1a2b3c4d-0005-4a1b-9c2d-000000000005',
     'Sơ cứu cơ bản khi trek: ba tình huống hay gặp nhất',
     E'Không phải ai cũng cần chứng chỉ sơ cứu, nhưng ba thứ này thì nên biết.\n\n**Bong gân cổ chân.** Dừng ngay, không cố đi tiếp. Chườm lạnh nếu có suối, băng ép, kê cao. Đừng xoa dầu nóng trong 24h đầu.\n\n**Mất nước và chuột rút.** Dấu hiệu sớm là nước tiểu sẫm màu và chóng mặt khi đứng dậy. Uống từng ngụm nhỏ kèm điện giải, không uống ừng ực.\n\n**Hạ thân nhiệt.** Run không kiểm soát, nói lắp, lơ mơ. Thay hết đồ ướt, ủ ấm bằng túi ngủ, cho uống nước ấm ngọt. Đây là tình huống nguy hiểm nhất và hay bị xem nhẹ nhất.\n\nMang theo một túi sơ cứu nhỏ luôn rẻ hơn hậu quả của việc không có nó.',
     'https://res.cloudinary.com/demo/image/upload/v1/treksphere/blogs/so-cuu-trek.jpg',
     1567, 'PUBLISHED', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '8 days'),

    ('9d0e1f2a-0009-4d9e-8f10-000000000009', '1a2b3c4d-0002-4a1b-9c2d-000000000002',
     'Bản nháp: review giày trek tầm giá dưới 2 triệu',
     E'Đang viết dở, còn thiếu phần đo độ bám trên đá ướt.\n\nDanh sách dự kiến so sánh: 4 mẫu phổ biến ở tầm giá này, chấm theo độ bám, độ bền chỉ khâu, khả năng chống nước và độ ôm cổ chân.\n\nCần đi thêm ít nhất một cung nữa trước khi kết luận.',
     NULL,
     0, 'DRAFT', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '5 days'),

    ('9d0e1f2a-0010-4d9e-8f10-000000000010', '1a2b3c4d-0006-4a1b-9c2d-000000000006',
     'Mẹo tiết kiệm chi phí khi đi trek cuối tuần',
     E'Bài này đang bị ẩn do có nội dung quảng cáo dịch vụ bên thứ ba chưa được kiểm chứng.\n\nNội dung gốc chia sẻ về cách ghép xe, thuê đồ thay vì mua, và chọn homestay giá rẻ.',
     NULL,
     89, 'HIDDEN', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '3 days');

-- ============================================================================
-- BLOG_COMMENT
-- ============================================================================
INSERT INTO blog_comment (
    blog_comment_id, blog_id, user_id, parent_comment_id, content, status,
    is_deleted, created_by, created_at
) VALUES
    -- Blog 1: Fansipan
    ('0e1f2a3b-0001-4e0f-8021-000000000001', '9d0e1f2a-0001-4d9e-8f10-000000000001',
     '1a2b3c4d-0002-4a1b-9c2d-000000000002', NULL,
     'Ý số 3 đúng quá. Mình cũng cố bám theo porter rồi đuối ngay trưa ngày đầu.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '41 days'),
    ('0e1f2a3b-0002-4e0f-8021-000000000002', '9d0e1f2a-0001-4d9e-8f10-000000000001',
     '1a2b3c4d-0001-4a1b-9c2d-000000000001', '0e1f2a3b-0001-4e0f-8021-000000000001',
     'Porter họ đi cung này cả trăm lần rồi bạn ơi, mình không theo được đâu.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '41 days'),
    ('0e1f2a3b-0003-4e0f-8021-000000000003', '9d0e1f2a-0001-4d9e-8f10-000000000001',
     '1a2b3c4d-0005-4a1b-9c2d-000000000005', NULL,
     'Bổ sung thêm: nên mang một đôi tất dự phòng để riêng trong túi chống nước.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '39 days'),
    ('0e1f2a3b-0004-4e0f-8021-000000000004', '9d0e1f2a-0001-4d9e-8f10-000000000001',
     '1a2b3c4d-0006-4a1b-9c2d-000000000006', NULL,
     'Cho mình hỏi cung Trạm Tôn với cung Cát Cát thì cái nào dễ hơn ạ?',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    ('0e1f2a3b-0005-4e0f-8021-000000000005', '9d0e1f2a-0001-4d9e-8f10-000000000001',
     '1a2b3c4d-0003-4a1b-9c2d-000000000003', '0e1f2a3b-0004-4e0f-8021-000000000004',
     'Trạm Tôn ngắn và dễ hơn đáng kể. Cát Cát dài hơn khoảng 8km và dốc liên tục.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '30 days'),

    -- Blog 2: Tà Năng checklist
    ('0e1f2a3b-0006-4e0f-8021-000000000006', '9d0e1f2a-0002-4d9e-8f10-000000000002',
     '1a2b3c4d-0001-4a1b-9c2d-000000000001', NULL,
     'Phần "đừng mang loa bluetooth" nên in đậm và phóng to.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '34 days'),
    ('0e1f2a3b-0007-4e0f-8021-000000000007', '9d0e1f2a-0002-4d9e-8f10-000000000002',
     '1a2b3c4d-0006-4a1b-9c2d-000000000006', NULL,
     'Viên lọc nước bạn dùng loại nào vậy? Mình tìm mãi không biết mua ở đâu.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '33 days'),
    ('0e1f2a3b-0008-4e0f-8021-000000000008', '9d0e1f2a-0002-4d9e-8f10-000000000002',
     '1a2b3c4d-0005-4a1b-9c2d-000000000005', '0e1f2a3b-0007-4e0f-8021-000000000007',
     'Loại viên aquatabs, các shop đồ dã ngoại lớn đều có. Nhớ đợi đủ 30 phút trước khi uống.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '33 days'),

    -- Blog 3: Porter ratio
    ('0e1f2a3b-0009-4e0f-8021-000000000009', '9d0e1f2a-0003-4d9e-8f10-000000000003',
     '1a2b3c4d-0005-4a1b-9c2d-000000000005', NULL,
     'Minh bạch như này thì đắt hơn 15% là hợp lý.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '27 days'),
    ('0e1f2a3b-0010-4e0f-8021-000000000010', '9d0e1f2a-0003-4d9e-8f10-000000000003',
     '1a2b3c4d-0002-4a1b-9c2d-000000000002', NULL,
     'Spam quảng cáo dịch vụ không liên quan.',
     'HIDDEN', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '26 days'),

    -- Blog 4: Langbiang
    ('0e1f2a3b-0011-4e0f-8021-000000000011', '9d0e1f2a-0004-4d9e-8f10-000000000004',
     '1a2b3c4d-0006-4a1b-9c2d-000000000006', NULL,
     'Đúng cái mình cần, cảm ơn bạn. Cuối tháng này mình thử.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '23 days'),
    ('0e1f2a3b-0012-4e0f-8021-000000000012', '9d0e1f2a-0004-4d9e-8f10-000000000004',
     '1a2b3c4d-0004-4a1b-9c2d-000000000004', NULL,
     'Bên mình có tour cung này đi trong ngày nếu bạn cần hướng dẫn viên.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '22 days'),

    -- Blog 5: Hang Én
    ('0e1f2a3b-0013-4e0f-8021-000000000013', '9d0e1f2a-0005-4d9e-8f10-000000000005',
     '1a2b3c4d-0001-4a1b-9c2d-000000000001', NULL,
     'Chi tiết "chim én ồn vào sáng sớm" làm mình cười. Rất thật.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    ('0e1f2a3b-0014-4e0f-8021-000000000014', '9d0e1f2a-0005-4d9e-8f10-000000000005',
     '1a2b3c4d-0005-4a1b-9c2d-000000000005', NULL,
     'Lội suối 10 lần thì giày lội nước gần như bắt buộc, giày trek thường sẽ ướt suốt hành trình.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    ('0e1f2a3b-0015-4e0f-8021-000000000015', '9d0e1f2a-0005-4d9e-8f10-000000000005',
     '1a2b3c4d-0004-4a1b-9c2d-000000000004', '0e1f2a3b-0014-4e0f-8021-000000000014',
     'Chuẩn. Bên mình khuyến nghị mang cả hai đôi, giày trek để đi trong hang cho đỡ trơn.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '18 days'),

    -- Blog 6: Nhóm ghép
    ('0e1f2a3b-0016-4e0f-8021-000000000016', '9d0e1f2a-0006-4d9e-8f10-000000000006',
     '1a2b3c4d-0002-4a1b-9c2d-000000000002', NULL,
     'Ý "nói trước nếu bạn đi chậm" hay. Mình từng ngại nói và kết quả là cả nhóm mệt.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '15 days'),

    -- Blog 7: Lảo Thẩn
    ('0e1f2a3b-0017-4e0f-8021-000000000017', '9d0e1f2a-0007-4d9e-8f10-000000000007',
     '1a2b3c4d-0003-4a1b-9c2d-000000000003', NULL,
     'Hai chỉ số bạn dùng khá sát với kinh nghiệm dẫn tour của mình.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '11 days'),
    ('0e1f2a3b-0018-4e0f-8021-000000000018', '9d0e1f2a-0007-4d9e-8f10-000000000007',
     '1a2b3c4d-0006-4a1b-9c2d-000000000006', NULL,
     'Tháng 12 đi có lạnh quá không bạn?',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ('0e1f2a3b-0019-4e0f-8021-000000000019', '9d0e1f2a-0007-4d9e-8f10-000000000007',
     '1a2b3c4d-0001-4a1b-9c2d-000000000001', '0e1f2a3b-0018-4e0f-8021-000000000018',
     'Đêm khoảng 3-5 độ. Túi ngủ 0 độ trở xuống là ổn.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '10 days'),

    -- Blog 8: Sơ cứu
    ('0e1f2a3b-0020-4e0f-8021-000000000020', '9d0e1f2a-0008-4d9e-8f10-000000000008',
     '1a2b3c4d-0003-4a1b-9c2d-000000000003', NULL,
     'Phần hạ thân nhiệt rất đáng đọc. Đây là thứ gây tai nạn nhiều hơn người ta tưởng.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    ('0e1f2a3b-0021-4e0f-8021-000000000021', '9d0e1f2a-0008-4d9e-8f10-000000000008',
     '1a2b3c4d-0002-4a1b-9c2d-000000000002', NULL,
     'Lưu lại. Mình sẽ in ra bỏ vào túi sơ cứu.',
     'VISIBLE', FALSE, 'SYSTEM', CURRENT_TIMESTAMP - INTERVAL '6 days');
