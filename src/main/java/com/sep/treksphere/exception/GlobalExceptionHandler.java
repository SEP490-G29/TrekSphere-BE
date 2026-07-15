package com.sep.treksphere.exception;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.FieldErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException [{}]: {}", errorCode.getCode(), ex.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getHttpStatus(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> FieldErrorDetail.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        log.warn("Validation error: {}", errors);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .code(status.value())
                .message("Dữ liệu không hợp lệ")
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestCookieException(MissingRequestCookieException ex) {
        log.warn("MissingRequestCookieException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("BadCredentialsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(Exception ex) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập vào tài nguyên này"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Xảy ra lỗi. Vui lòng thử lại sau.."));
    }
}
