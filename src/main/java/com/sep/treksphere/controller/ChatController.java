package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
