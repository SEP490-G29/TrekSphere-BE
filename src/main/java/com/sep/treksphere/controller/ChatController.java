package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.ConversationCreateRequest;
import com.sep.treksphere.dto.request.MessageCreateRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.MessageResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.UserResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(
        name = "Chat Management",
        description = "Các API quản lý chat và hội thoại"
)
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ConversationService conversationService;

    @Operation(
            summary = "Danh sách phòng chat",
            description = "Lấy danh sách các phòng chat của người dùng, sắp xếp theo tin nhắn mới nhất."
    )
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaginationResponse<ConversationResponse>>> getConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationResponse<ConversationResponse> result =
                conversationService.getConversations(page, size, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.CONVERSATIONS_FETCHED_SUCCESS
        ));
    }

    @Operation(
            summary = "Tạo phòng chat mới",
            description = "Tạo phòng chat 1-1 (DIRECT) hoặc phòng chat nhóm (GROUP)."
    )
    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ConversationResponse result = conversationService.createConversation(request, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        result
                ));
    }

    @Operation(
            summary = "Kiểm tra phòng chat tồn tại",
            description = "Kiểm tra xem phòng chat đã tồn tại chưa (không tạo mới). Trả về thông tin phòng chat nếu có, ngược lại HTTP 204."
    )
    @PostMapping("/conversations/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConversationResponse>> checkConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ConversationResponse result = conversationService.checkConversation(request, userDetails);
        if (result != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    HttpStatus.OK,
                    result,
                    "Conversation found"
            ));
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Lịch sử tin nhắn",
            description = "Lấy lịch sử tin nhắn trong phòng chat, phân trang ngược theo thời gian."
    )
    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaginationResponse<MessageResponse>>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationResponse<MessageResponse> result =
                conversationService.getMessages(id, page, size, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MESSAGES_FETCHED_SUCCESS
        ));
    }

    @Operation(
            summary = "Gửi tin nhắn",
            description = "Gửi tin nhắn mới vào một cuộc hội thoại mà người dùng đang tham gia."
    )
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody MessageCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MessageResponse result = conversationService.sendMessage(request, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        result,
                        MessageConstant.MESSAGE_SENT_SUCCESS
                ));
    }

    @Operation(
            summary = "Đánh dấu tin nhắn đã đọc",
            description = "Đánh dấu tất cả tin nhắn chưa đọc do người khác gửi trong cuộc hội thoại là đã đọc."
    )
    @PutMapping("/conversations/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        conversationService.markMessagesAsRead(id, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                null,
                MessageConstant.MESSAGES_MARKED_READ_SUCCESS
        ));
    }

    @Operation(
            summary = "Xóa cuộc hội thoại",
            description = "Xóa cuộc hội thoại. Với DIRECT chat, thành viên nào cũng có thể xóa. Với GROUP chat, chỉ người tạo (chủ nhóm) mới có quyền xóa."
    )
    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        conversationService.deleteConversation(id, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                null,
                "Xóa cuộc hội thoại thành công"
        ));
    }

    @Operation(
            summary = "Đuổi thành viên khỏi nhóm chat",
            description = "Xóa một thành viên khỏi nhóm chat. Chỉ áp dụng cho GROUP chat và chỉ người tạo (chủ nhóm) mới có quyền thực hiện."
    )
    @DeleteMapping("/conversations/{id}/members/{memberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        conversationService.removeMember(id, memberId, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                null,
                "Đã xóa thành viên khỏi nhóm chat"
        ));
    }

    @Operation(
            summary = "Danh sách thành viên nhóm chat",
            description = "Lấy danh sách thành viên của một cuộc hội thoại."
    )
    @GetMapping("/conversations/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getConversationMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<UserResponse> result = conversationService.getConversationMembers(id, userDetails);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                "Lấy danh sách thành viên thành công"
        ));
    }
}
