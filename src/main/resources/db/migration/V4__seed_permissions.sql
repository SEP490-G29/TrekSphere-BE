-- Permission catalog cho toàn bộ chức năng hiện có và đã thiết kế (xem usecase_gap_analysis_v3.1.md).
-- Authority runtime suy ra từ resource+action: "{RESOURCE}_{ACTION}" (xem CustomUserDetails.getAuthorities()).

INSERT INTO permission (permission_id, resource, action, description) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'USER', 'VIEW', 'Xem danh sách/chi tiết user (Admin)'),
    ('a1000000-0000-4000-8000-000000000002', 'USER', 'MANAGE_STATUS', 'Khoá/mở khoá tài khoản user (Admin)'),
    ('a1000000-0000-4000-8000-000000000003', 'USER', 'VIEW_PUBLIC', 'Xem hồ sơ công khai của user khác'),
    ('a1000000-0000-4000-8000-000000000004', 'USER', 'PROFILE_MANAGE', 'Xem/sửa hồ sơ cá nhân của chính mình'),
    ('a1000000-0000-4000-8000-000000000005', 'ROLE', 'MANAGE', 'Quản lý role và gán permission cho role (Admin)'),
    ('a1000000-0000-4000-8000-000000000006', 'VENDOR', 'VIEW_ALL', 'Xem danh sách toàn bộ Vendor (Admin)'),
    ('a1000000-0000-4000-8000-000000000007', 'VENDOR', 'MANAGE_STATUS', 'Đổi trạng thái Vendor (Admin)'),
    ('a1000000-0000-4000-8000-000000000008', 'VENDOR', 'PROFILE_MANAGE', 'Xem/sửa hồ sơ Vendor của chính mình'),
    ('a1000000-0000-4000-8000-000000000009', 'VENDOR', 'VIEW_PUBLIC', 'Xem hồ sơ công khai của một Vendor'),
    ('a1000000-0000-4000-8000-00000000000a', 'VENDOR', 'STATISTICS_VIEW', 'Vendor xem thống kê tour/nhóm ghép của mình'),
    ('a1000000-0000-4000-8000-00000000000b', 'VENDOR_APPLICATION', 'SUBMIT', 'Nộp/sửa/resubmit đơn đăng ký Vendor'),
    ('a1000000-0000-4000-8000-00000000000c', 'VENDOR_APPLICATION', 'VIEW', 'Xem danh sách/chi tiết đơn đăng ký Vendor (Admin)'),
    ('a1000000-0000-4000-8000-00000000000d', 'VENDOR_APPLICATION', 'DECIDE', 'Duyệt/từ chối đơn đăng ký Vendor (Admin)'),
    ('a1000000-0000-4000-8000-00000000000e', 'TOUR', 'MANAGE_OWN', 'CRUD tour, lịch khởi hành, checkpoint của Vendor'),
    ('a1000000-0000-4000-8000-00000000000f', 'TOUR', 'PUBLISH', 'Vendor tự publish tour'),
    ('a1000000-0000-4000-8000-000000000010', 'TOUR', 'HIDE_UNHIDE', 'Ẩn/hiện tour (Admin kiểm duyệt hoặc Vendor tự ẩn)'),
    ('a1000000-0000-4000-8000-000000000011', 'MATCHING_GROUP', 'CREATE', 'Tạo nhóm ghép bạn đồng hành'),
    ('a1000000-0000-4000-8000-000000000012', 'MATCHING_GROUP', 'MANAGE_OWN', 'Chủ nhóm quản lý nhóm ghép của mình (sửa, duyệt/từ chối thành viên, xoá thành viên, giải tán)'),
    ('a1000000-0000-4000-8000-000000000013', 'MATCHING_GROUP', 'PARTICIPATE', 'Tham gia nhóm ghép (xin vào, huỷ yêu cầu, rời nhóm)'),
    ('a1000000-0000-4000-8000-000000000014', 'MATCHING_GROUP', 'VOTE', 'Tạo/bỏ phiếu/xem kết quả bỏ phiếu trong nhóm ghép'),
    ('a1000000-0000-4000-8000-000000000015', 'GROUP_TRIP', 'MANAGE', 'Quản lý không gian chuyến đi: lịch trình, chi phí, checklist, moments, Q&A'),
    ('a1000000-0000-4000-8000-000000000016', 'GROUP_TRIP', 'PEER_REVIEW', 'Đánh giá đồng đội sau chuyến đi'),
    ('a1000000-0000-4000-8000-000000000017', 'GROUP_TRIP', 'SOS', 'Gửi/xem/resolve tín hiệu SOS trong nhóm'),
    ('a1000000-0000-4000-8000-000000000018', 'BLOG', 'MANAGE_OWN', 'Tạo/sửa/ẩn blog của chính mình'),
    ('a1000000-0000-4000-8000-000000000019', 'BLOG', 'COMMENT', 'Bình luận blog'),
    ('a1000000-0000-4000-8000-00000000001a', 'CHAT', 'PARTICIPATE', 'Sử dụng chat: xem/tạo hội thoại, gửi tin nhắn'),
    ('a1000000-0000-4000-8000-00000000001b', 'NOTIFICATION', 'VIEW', 'Xem thông báo của chính mình'),
    ('a1000000-0000-4000-8000-00000000001c', 'REPORT', 'SUBMIT', 'Report nội dung vi phạm'),
    ('a1000000-0000-4000-8000-00000000001d', 'REPORT', 'VIEW', 'Xem danh sách/chi tiết report (Admin)'),
    ('a1000000-0000-4000-8000-00000000001e', 'REPORT', 'RESOLVE', 'Xử lý/resolve report (Admin)');

-- ADMIN: quản trị hệ thống + năng lực chung (profile, chat, notification)
INSERT INTO role_permission (role_id, permission_id)
SELECT 'a85194bd-7aa5-4d85-9400-1d145000675c', permission_id FROM permission
WHERE (resource, action) IN (
    ('USER', 'VIEW'), ('USER', 'MANAGE_STATUS'), ('USER', 'VIEW_PUBLIC'), ('USER', 'PROFILE_MANAGE'),
    ('ROLE', 'MANAGE'),
    ('VENDOR', 'VIEW_ALL'), ('VENDOR', 'MANAGE_STATUS'), ('VENDOR', 'VIEW_PUBLIC'),
    ('VENDOR_APPLICATION', 'VIEW'), ('VENDOR_APPLICATION', 'DECIDE'),
    ('TOUR', 'HIDE_UNHIDE'),
    ('CHAT', 'PARTICIPATE'), ('NOTIFICATION', 'VIEW'),
    ('REPORT', 'VIEW'), ('REPORT', 'RESOLVE')
);

-- VENDOR: quản lý tour/hồ sơ vendor của mình + năng lực chung
INSERT INTO role_permission (role_id, permission_id)
SELECT '6162afa8-8a00-4477-9e5e-5ce2ddb3258d', permission_id FROM permission
WHERE (resource, action) IN (
    ('USER', 'VIEW_PUBLIC'), ('USER', 'PROFILE_MANAGE'),
    ('VENDOR', 'PROFILE_MANAGE'), ('VENDOR', 'VIEW_PUBLIC'), ('VENDOR', 'STATISTICS_VIEW'),
    ('TOUR', 'MANAGE_OWN'), ('TOUR', 'PUBLISH'), ('TOUR', 'HIDE_UNHIDE'),
    ('CHAT', 'PARTICIPATE'), ('NOTIFICATION', 'VIEW'),
    ('REPORT', 'SUBMIT')
);

-- TREKKER: nộp đơn vendor, ghép nhóm, group-trip, blog + năng lực chung
INSERT INTO role_permission (role_id, permission_id)
SELECT 'd16abaf9-b60a-4d43-bcdd-8ce760b17041', permission_id FROM permission
WHERE (resource, action) IN (
    ('USER', 'VIEW_PUBLIC'), ('USER', 'PROFILE_MANAGE'),
    ('VENDOR', 'VIEW_PUBLIC'),
    ('VENDOR_APPLICATION', 'SUBMIT'),
    ('MATCHING_GROUP', 'CREATE'), ('MATCHING_GROUP', 'MANAGE_OWN'), ('MATCHING_GROUP', 'PARTICIPATE'), ('MATCHING_GROUP', 'VOTE'),
    ('GROUP_TRIP', 'MANAGE'), ('GROUP_TRIP', 'PEER_REVIEW'), ('GROUP_TRIP', 'SOS'),
    ('BLOG', 'MANAGE_OWN'), ('BLOG', 'COMMENT'),
    ('CHAT', 'PARTICIPATE'), ('NOTIFICATION', 'VIEW'),
    ('REPORT', 'SUBMIT')
);
