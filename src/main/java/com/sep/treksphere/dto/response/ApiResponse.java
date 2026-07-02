package com.sep.treksphere.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private List<FieldErrorDetail> errors;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message("Thành Công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(HttpStatus status, T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(status.value())
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> validationError(String message, List<FieldErrorDetail> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .errors(errors)
                .build();
    }
}
