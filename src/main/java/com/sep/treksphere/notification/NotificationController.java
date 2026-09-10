package com.sep.treksphere.notification;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Các API quản lý thông báo trong ứng dụng")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Danh sách thông báo",
            description = "Lấy danh sách thông báo của người dùng hiện tại, có thể lọc theo trạng thái đã đọc."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ResponseEntity<ApiResponse<PaginationResponse<NotificationResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationResponse<NotificationResponse> result = notificationService.list(
                userDetails.getUser().getUserId(), page, size, isRead);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, result, MessageConstant.NOTIFICATIONS_FETCHED_SUCCESS));
    }

    @Operation(
            summary = "Số thông báo chưa đọc",
            description = "Lấy số lượng thông báo chưa đọc của người dùng hiện tại, dùng để hiển thị badge."
    )
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationService.unreadCount(userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, count, MessageConstant.NOTIFICATION_UNREAD_COUNT_FETCHED_SUCCESS));
    }

    @Operation(
            summary = "Đánh dấu 1 thông báo đã đọc",
            description = "Đánh dấu 1 thông báo của người dùng hiện tại là đã đọc."
    )
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAsRead(id, userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATION_MARKED_READ_SUCCESS));
    }

    @Operation(
            summary = "Đánh dấu tất cả thông báo đã đọc",
            description = "Đánh dấu toàn bộ thông báo chưa đọc của người dùng hiện tại là đã đọc."
    )
    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATIONS_MARKED_ALL_READ_SUCCESS));
    }

    @Operation(
            summary = "Xóa thông báo",
            description = "Xóa (ẩn) 1 thông báo của người dùng hiện tại."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.delete(id, userDetails.getUser().getUserId());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, null, MessageConstant.NOTIFICATION_DELETED_SUCCESS));
    }
}
