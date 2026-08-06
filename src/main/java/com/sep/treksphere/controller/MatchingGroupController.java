package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.request.OwnedMatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.MatchingGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups")
@RequiredArgsConstructor
@Tag(name = "Matching Group", description = "Các API liên quan đến ghép nhóm đồng hành")
public class MatchingGroupController {

    private final MatchingGroupService matchingGroupService;

    @Operation(
        summary = "Tìm kiếm các nhóm ghép bạn đồng hành",
        description = "Lấy danh sách các nhóm còn mở ghép thành viên của Tour đang public. " +
                "Cho phép tìm theo tên nhóm hoặc tên Tour, lọc theo Tour và ngày Trekker dự kiến đi; " +
                "ngày dự kiến không phụ thuộc lịch khởi hành của Tour."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingGroupResponse>>> getMatchingGroups(
            @Valid @ParameterObject @ModelAttribute MatchingGroupFilterRequest filter) {
        PaginationResponse<MatchingGroupResponse> result = matchingGroupService.getMatchingGroups(filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUPS_FETCHED_SUCCESS));
    }

    @Operation(
        summary = "Lấy các nhóm ghép do Trekker hiện tại quản lý",
        description = "Trả về tất cả nhóm do Trekker hiện tại sở hữu, bao gồm lịch sử nhóm đã giải tán và không giới hạn theo ngày dự kiến đi. " +
                "Có thể lọc theo trạng thái và tìm theo tên nhóm hoặc tên Tour."
    )
    @GetMapping("/owned")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingGroupResponse>>> getOwnedMatchingGroups(
            @Valid @ParameterObject @ModelAttribute OwnedMatchingGroupFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MatchingGroupResponse> result =
                matchingGroupService.getOwnedMatchingGroups(filter, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MATCHING_GROUPS_FETCHED_SUCCESS
        ));
    }

    @Operation(
        summary = "Xem chi tiết nhóm ghép bạn đồng hành",
        description = "Lấy thông tin public của nhóm ghép thuộc Tour đang public, bao gồm các thành viên đã được duyệt. " +
                "Nếu người xem đã đăng nhập, response có thêm trạng thái tham gia và quyền join/leave của người đó."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> getMatchingGroupById(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.getMatchingGroupById(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_FETCHED_SUCCESS));
    }

    @Operation(
        summary = "Tạo một nhóm ghép bạn đồng hành mới",
        description = "Cho phép Trekker tạo nhóm ghép mới cho Tour đã được duyệt. " +
                "Ngày đi dự kiến phải ở tương lai và không bắt buộc trùng với lịch khởi hành của Tour."
    )
    @PostMapping
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> createMatchingGroup(
            @Valid @RequestBody MatchingGroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.createMatchingGroup(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.MATCHING_GROUP_CREATED_SUCCESS));
    }

    @Operation(
        summary = "Gửi yêu cầu xin tham gia vào nhóm ghép",
        description = "Cho phép Trekker gửi yêu cầu xin tham gia vào nhóm ghép bạn đồng hành đang mở."
    )
    @PostMapping("/{groupId}/join")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> joinMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.joinMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_JOIN_REQUESTED_SUCCESS));
    }

    @Operation(
        summary = "Lấy danh sách yêu cầu tham gia nhóm ghép",
        description = "Cho phép Trưởng nhóm (Owner) xem danh sách yêu cầu tham gia của một nhóm cụ thể để duyệt hoặc từ chối."
    )
    @GetMapping("/{groupId}/join-requests")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingMemberResponse>>> getJoinRequests(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @RequestParam(defaultValue = "PENDING") JoinStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MatchingMemberResponse> result =
                matchingGroupService.getJoinRequests(groupId, status, page, size, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MATCHING_JOIN_REQUESTS_FETCHED_SUCCESS
        ));
    }

    @Operation(
        summary = "Duyệt thành viên xin vào nhóm",
        description = "Cho phép Trưởng nhóm (Leader/Owner) phê duyệt yêu cầu tham gia nhóm ghép của thành viên đang ở trạng thái PENDING."
    )
    @PutMapping("/members/{memberId}/approve")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> approveMember(
            @Parameter(description = "UUID của bản ghi thành viên cần duyệt") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.approveMember(memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_APPROVED_SUCCESS));
    }

    @Operation(
        summary = "Từ chối thành viên xin vào nhóm",
        description = "Cho phép Trưởng nhóm (Leader/Owner) từ chối yêu cầu tham gia nhóm ghép của thành viên đang ở trạng thái PENDING."
    )
    @PutMapping("/members/{memberId}/reject")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> rejectMember(
            @Parameter(description = "UUID của bản ghi thành viên cần từ chối") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.rejectMember(memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_REJECTED_SUCCESS));
    }

    @Operation(
        summary = "Rời khỏi nhóm ghép hiện tại",
        description = "Cho phép thành viên (ACCEPTED hoặc PENDING) rời khỏi nhóm ghép. Nếu trước đó nhóm đầy (FULL), nhóm sẽ tự động mở lại (OPEN) để tiếp tục ghép."
    )
    @PostMapping("/{groupId}/leave")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> leaveMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.leaveMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_LEFT_SUCCESS));
    }

    @Operation(
        summary = "Giải tán nhóm ghép",
        description = "Cho phép Trưởng nhóm (Owner) giải tán nhóm ghép bạn đồng hành. Hệ thống sẽ ẩn nhóm và các thành viên bằng cơ chế soft-delete."
    )
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('TREKKER')")
    public ResponseEntity<ApiResponse<Void>> disbandMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        matchingGroupService.disbandMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.MATCHING_GROUP_DISBANDED_SUCCESS));
    }
}
