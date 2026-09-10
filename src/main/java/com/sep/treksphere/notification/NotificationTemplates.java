package com.sep.treksphere.notification;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bảng tra tiêu đề/nội dung cho từng {@link NotificationEventType}, dùng {@link String#format(String, Object...)}.
 * Thêm loại thông báo mới: chỉ cần thêm 1 dòng vào {@code TEMPLATES} bên dưới — xem cookbook trong
 * draft/notification_system_plan.md mục 4.
 */
public final class NotificationTemplates {

    public record Resolved(String title, String content) {
    }

    private record TemplatePair(String titleTemplate, String contentTemplate) {
    }

    private static final Map<NotificationEventType, TemplatePair> TEMPLATES = new EnumMap<>(NotificationEventType.class);

    static {
        TEMPLATES.put(NotificationEventType.TOUR_HIDDEN_VIOLATION, new TemplatePair(
                "Tour bị ẩn do vi phạm",
                "Tour \"%s\" đã bị ẩn. Lý do: %s"));

        TEMPLATES.put(NotificationEventType.GROUP_JOIN_REQUEST, new TemplatePair(
                "Yêu cầu tham gia nhóm mới",
                "%s muốn tham gia nhóm \"%s\" của bạn."));
        TEMPLATES.put(NotificationEventType.GROUP_MEMBER_APPROVED, new TemplatePair(
                "Yêu cầu được chấp nhận",
                "Bạn đã được chấp nhận vào nhóm \"%s\"."));
        TEMPLATES.put(NotificationEventType.GROUP_MEMBER_REJECTED, new TemplatePair(
                "Yêu cầu bị từ chối",
                "Yêu cầu tham gia nhóm \"%s\" bị từ chối."));
        TEMPLATES.put(NotificationEventType.GROUP_MEMBER_LEFT, new TemplatePair(
                "Thành viên rời nhóm",
                "%s đã rời khỏi nhóm \"%s\"."));
        TEMPLATES.put(NotificationEventType.GROUP_DISBANDED, new TemplatePair(
                "Nhóm đã giải tán",
                "Nhóm \"%s\" đã bị giải tán."));

        // Nội dung có 2 biến thể cấu trúc khác nhau (top-level vs reply) nên được
        // BlogCommentService dựng sẵn thành 1 câu hoàn chỉnh rồi truyền vào đây.
        TEMPLATES.put(NotificationEventType.BLOG_COMMENT_ADDED, new TemplatePair(
                "Bình luận mới",
                "%s"));

        TEMPLATES.put(NotificationEventType.NEW_MESSAGE, new TemplatePair(
                "Tin nhắn mới",
                "%s: %s"));

        TEMPLATES.put(NotificationEventType.TOUR_UNHIDDEN, new TemplatePair(
                "Tour đã hiển thị lại",
                "Tour \"%s\" đã được gỡ ẩn."));
        TEMPLATES.put(NotificationEventType.SCHEDULE_UPDATED, new TemplatePair(
                "Lịch trình đã bị huỷ",
                "Lịch khởi hành \"%s\" ngày %s đã bị huỷ."));

        TEMPLATES.put(NotificationEventType.VENDOR_APPLICATION_SUBMITTED, new TemplatePair(
                "Đơn đăng ký nhà cung cấp mới",
                "%s vừa nộp đơn đăng ký trở thành nhà cung cấp."));
        TEMPLATES.put(NotificationEventType.VENDOR_APPLICATION_APPROVED, new TemplatePair(
                "Đơn đăng ký đã được duyệt",
                "Đơn đăng ký nhà cung cấp của bạn đã được phê duyệt."));
        TEMPLATES.put(NotificationEventType.VENDOR_APPLICATION_REJECTED, new TemplatePair(
                "Đơn đăng ký bị từ chối",
                "Đơn đăng ký của bạn đã bị từ chối. Lý do: %s"));
        TEMPLATES.put(NotificationEventType.VENDOR_STATUS_CHANGED, new TemplatePair(
                "Trạng thái tài khoản nhà cung cấp thay đổi",
                "Tài khoản của bạn đã chuyển sang trạng thái %s."));

        TEMPLATES.put(NotificationEventType.BLOG_HIDDEN, new TemplatePair(
                "Bài viết đã bị ẩn",
                "Bài viết \"%s\" đã bị ẩn bởi quản trị viên."));
        // Nội dung tuỳ loại (blog/comment) nên được caller dựng sẵn thành 1 câu hoàn chỉnh.
        TEMPLATES.put(NotificationEventType.BLOG_DELETED, new TemplatePair(
                "Nội dung đã bị xoá",
                "%s"));

        TEMPLATES.put(NotificationEventType.CONVERSATION_MEMBER_ADDED, new TemplatePair(
                "Bạn đã được thêm vào cuộc trò chuyện",
                "%s đã thêm bạn vào \"%s\"."));

        TEMPLATES.put(NotificationEventType.REPORT_SUBMITTED, new TemplatePair(
                "Báo cáo mới",
                "%s vừa gửi báo cáo về %s."));
        TEMPLATES.put(NotificationEventType.REPORT_RESOLVED, new TemplatePair(
                "Báo cáo đã được xử lý",
                "Báo cáo của bạn đã được xử lý: %s."));

        TEMPLATES.put(NotificationEventType.USER_STATUS_CHANGED, new TemplatePair(
                "Tài khoản đã thay đổi trạng thái",
                "Tài khoản của bạn đã %s bởi quản trị viên."));
    }

    private NotificationTemplates() {
    }

    public static Resolved resolve(NotificationEventType eventType, Object... args) {
        TemplatePair pair = TEMPLATES.get(eventType);
        if (pair == null) {
            throw new IllegalStateException(
                    "Chưa đăng ký template cho NotificationEventType: " + eventType);
        }
        return new Resolved(
                String.format(pair.titleTemplate(), args),
                String.format(pair.contentTemplate(), args));
    }
}
