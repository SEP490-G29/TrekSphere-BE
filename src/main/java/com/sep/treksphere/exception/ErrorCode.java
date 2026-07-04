package com.sep.treksphere.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import com.sep.treksphere.constant.MessageConstant;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR, MessageConstant.SYSTEM_ERROR_UNKNOWN),
    INVALID_KEY(9998, HttpStatus.BAD_REQUEST, MessageConstant.INVALID_MESSAGE_KEY),
    VALIDATION_ERROR(9001, HttpStatus.BAD_REQUEST, MessageConstant.VALIDATION_ERROR_MSG),

    UNAUTHORIZED(1001, HttpStatus.UNAUTHORIZED, MessageConstant.UNAUTHORIZED_ACTION),
    ACCESS_DENIED(1002, HttpStatus.FORBIDDEN, MessageConstant.ACCESS_DENIED),
    USER_NOT_FOUND(1003, HttpStatus.NOT_FOUND, MessageConstant.USER_NOT_FOUND),
    EMAIL_EXISTED(1004, HttpStatus.BAD_REQUEST, MessageConstant.EMAIL_EXISTED),
    WRONG_PASSWORD(1005, HttpStatus.BAD_REQUEST, MessageConstant.WRONG_PASSWORD),
    USER_NOT_ACTIVE(1006, HttpStatus.FORBIDDEN, MessageConstant.USER_NOT_ACTIVE_OR_LOCKED),
    EMAIL_NOT_VERIFIED(1007, HttpStatus.FORBIDDEN, MessageConstant.EMAIL_NOT_VERIFIED),
    INVALID_TOKEN(1008, HttpStatus.UNAUTHORIZED, MessageConstant.INVALID_TOKEN),
    ROLE_NOT_FOUND(1009, HttpStatus.INTERNAL_SERVER_ERROR, MessageConstant.ROLE_NOT_FOUND),

    // Upload
    UPLOAD_FAILED(2001, HttpStatus.INTERNAL_SERVER_ERROR, MessageConstant.UPLOAD_FAILED),
    FILE_TOO_LARGE(2002, HttpStatus.BAD_REQUEST, MessageConstant.FILE_TOO_LARGE),
    INVALID_FILE_FORMAT(2003, HttpStatus.BAD_REQUEST, MessageConstant.INVALID_FILE_FORMAT),
    
    // Email
    EMAIL_SEND_FAILED(3001, HttpStatus.INTERNAL_SERVER_ERROR, MessageConstant.EMAIL_SEND_FAILED),

    // Tour
    TOUR_NOT_FOUND(3001, HttpStatus.NOT_FOUND, MessageConstant.TOUR_NOT_FOUND),

    // Blog
    BLOG_NOT_FOUND(4001, HttpStatus.NOT_FOUND, MessageConstant.BLOG_NOT_FOUND);


    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
