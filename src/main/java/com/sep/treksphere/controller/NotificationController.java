package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.NotificationResponse;
import com.sep.treksphere.dto.response.NotificationUnreadCountResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Các API quản lý thông báo người dùng")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo của người dùng hiện tại")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaginationResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<NotificationResponse> result =
                notificationService.getMyNotifications(page, size, isRead, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, result, MessageConstant.NOTIFICATIONS_FETCHED_SUCCESS));
    }

    @Operation(summary = "Lấy số lượng thông báo chưa đọc")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        NotificationUnreadCountResponse result = notificationService.getUnreadCount(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, result, MessageConstant.NOTIFICATION_UNREAD_COUNT_FETCHED_SUCCESS));
    }

    @Operation(summary = "Đánh dấu một thông báo là đã đọc")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATION_MARKED_READ_SUCCESS));
    }

    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATIONS_MARKED_READ_SUCCESS));
    }

    @Operation(summary = "Xóa mềm một thông báo")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotification(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATION_DELETED_SUCCESS));
    }
}
