package com.sep.treksphere.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định"),
    INVALID_KEY(9998, HttpStatus.BAD_REQUEST, "Invalid message key"),
    VALIDATION_ERROR(9001, HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    UNAUTHORIZED(1001, HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện chức năng này"),
    ACCESS_DENIED(1002, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này"),
    USER_NOT_FOUND(1003, HttpStatus.NOT_FOUND, "Người dùng không tồn tại"),
    EMAIL_EXISTED(1004, HttpStatus.BAD_REQUEST, "Email đã tồn tại"),
    WRONG_PASSWORD(1005, HttpStatus.BAD_REQUEST, "Mật khẩu không chính xác"),
    USER_NOT_ACTIVE(1006, HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt hoặc bị khóa"),
    EMAIL_NOT_VERIFIED(1007, HttpStatus.FORBIDDEN, "Vui lòng xác thực email trước khi đăng nhập"),
    INVALID_TOKEN(1008, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn"),
    ROLE_NOT_FOUND(1009, HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy vai trò mặc định trong hệ thống"),

    // Upload
    UPLOAD_FAILED(2001, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tải file lên hệ thống"),
    FILE_TOO_LARGE(2002, HttpStatus.BAD_REQUEST, "Kích thước file vượt quá giới hạn cho phép (10MB)"),
    INVALID_FILE_FORMAT(2003, HttpStatus.BAD_REQUEST, "Định dạng file không được hỗ trợ"),

    // Tour
    TOUR_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "Tour không tồn tại"),

    // Blog
    BLOG_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "Bài viết không tồn tại");


    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
