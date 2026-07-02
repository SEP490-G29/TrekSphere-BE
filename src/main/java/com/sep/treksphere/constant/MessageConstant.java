package com.sep.treksphere.constant;

public class MessageConstant {
    private MessageConstant() {
        // Private constructor to prevent instantiation
    }

    // Auth Messages
    public static final String USER_NOT_FOUND = "Người dùng không tồn tại";
    public static final String USER_NOT_LOGGED_IN = "Người dùng chưa đăng nhập";
    public static final String CURRENT_PASSWORD_INCORRECT = "Mật khẩu hiện tại không chính xác";
    public static final String NEW_PASSWORD_SAME_AS_OLD = "Mật khẩu mới không được trùng với mật khẩu cũ";
    public static final String PASSWORD_CHANGED_SUCCESSFULLY = "Mật khẩu đã được thay đổi thành công.";
    public static final String RESET_LINK_SENT_SUCCESSFULLY = "Nếu email hợp lệ, một đường link đặt lại mật khẩu đã được gửi đến email của bạn.";
    public static final String PASSWORD_RESET_SUCCESSFULLY = "Mật khẩu đã được đặt lại thành công.";
    
    // Validation Messages
    public static final String CURRENT_PASSWORD_REQUIRED = "Vui lòng nhập mật khẩu hiện tại";
    public static final String NEW_PASSWORD_REQUIRED = "Vui lòng nhập mật khẩu mới";
    public static final String PASSWORD_MIN_LENGTH = "Mật khẩu phải có ít nhất 8 ký tự";
}
