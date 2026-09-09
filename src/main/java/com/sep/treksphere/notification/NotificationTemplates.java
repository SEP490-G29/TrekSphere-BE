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
