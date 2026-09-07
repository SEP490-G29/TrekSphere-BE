package com.sep.treksphere.matching.controller;

import com.sep.treksphere.matching.enums.JoinStatus;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingGroupFilterRequest;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.service.MatchingGroupService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups")
@RequiredArgsConstructor
@Tag(name = "Matching Group", description = "Các API liên quan đến ghép nhóm đồng hành")
public class MatchingGroupController {

    private final MatchingGroupService matchingGroupService;

    @Operation(
        summary = "Tìm kiếm các nhóm ghép bạn đồng hành",
        description = "Lấy danh sách các nhóm còn mở ghép thành viên của Tour được phê duyệt hoặc Custom Journey độc lập. " +
                "Cho phép lọc theo loại nguồn (sourceType: TOUR, CUSTOM_JOURNEY), Tour ID, độ khó (difficulty), địa điểm (location), " +
                "ngày đi dự kiến (targetDate/targetDateFrom/targetDateTo), và tình trạng chỗ trống (availableSlotsOnly)."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingGroupResponse>>> getMatchingGroups(
            @Valid @ParameterObject @ModelAttribute MatchingGroupFilterRequest filter) {
        PaginationResponse<MatchingGroupResponse> result = matchingGroupService.getMatchingGroups(filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUPS_FETCHED_SUCCESS));
    }

    @Operation(
        summary = "Lấy danh sách các nhóm ghép tôi đã tham gia hoặc làm chủ",
        description = "Trả về các nhóm do Trekker hiện tại sở hữu (Leader) hoặc đã tham gia với tư cách thành viên được chấp nhận (Accepted Member), bao gồm lịch sử nhóm đã giải tán và không giới hạn theo ngày dự kiến đi. " +
                "Có thể lọc theo trạng thái và tìm theo tên nhóm, tên Tour hoặc tiêu đề Custom Journey."
    )
    @GetMapping(value = {"/my-groups", "/my-group"})
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingGroupResponse>>> getMyMatchingGroups(
            @Valid @ParameterObject @ModelAttribute MyMatchingGroupFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MatchingGroupResponse> result =
                matchingGroupService.getMyMatchingGroups(filter, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MATCHING_GROUPS_FETCHED_SUCCESS
        ));
    }

    @Operation(
        summary = "Xem chi tiết nhóm ghép bạn đồng hành",
        description = "Lấy thông tin public của nhóm ghép (Tour đã duyệt hoặc Custom Journey), bao gồm danh sách thành viên đã được duyệt, thông tin hành trình / checkpoint và chi phí ước tính. " +
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
        description = "Cho phép Trekker tạo nhóm từ một Tour đã duyệt hoặc một Custom Journey độc lập. " +
                "Request phải chọn đúng một sourceType, thời điểm khởi hành dự kiến phải ở tương lai; " +
                "Group, Leader và GroupTrip PLANNED được tạo atomically."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_CREATE')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> createMatchingGroup(
            @Valid @RequestBody MatchingGroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.createMatchingGroup(
                request,
                userDetails.getUser().getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.MATCHING_GROUP_CREATED_SUCCESS));
    }

    @Operation(
        summary = "Gửi yêu cầu xin tham gia vào nhóm ghép",
        description = "Cho phép Trekker gửi yêu cầu xin tham gia vào nhóm ghép bạn đồng hành đang mở."
    )
    @PostMapping("/{groupId}/join")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> joinMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.joinMatchingGroup(groupId, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED,
                result,
                MessageConstant.MATCHING_GROUP_JOIN_REQUESTED_SUCCESS
        ));
    }

    @Operation(
        summary = "Lấy danh sách yêu cầu tham gia nhóm ghép",
        description = "Cho phép Trưởng nhóm (Owner) xem danh sách yêu cầu tham gia của một nhóm cụ thể để duyệt hoặc từ chối."
    )
    @GetMapping("/{groupId}/join-requests")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<PaginationResponse<MatchingMemberResponse>>> getJoinRequests(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Valid @ParameterObject @ModelAttribute MatchingJoinRequestFilter filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MatchingMemberResponse> result =
                matchingGroupService.getJoinRequests(groupId, filter, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MATCHING_JOIN_REQUESTS_FETCHED_SUCCESS
        ));
    }

    @Operation(
        summary = "Xem các yêu cầu tham gia nhóm ghép của tôi",
        description = "Cho phép Trekker xem các yêu cầu tham gia nhóm ghép của chính mình. " +
                "Mặc định trả về yêu cầu ở tất cả trạng thái; Trekker có thể tùy chọn lọc theo JoinStatus."
    )
    @GetMapping("/join-requests/me")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<PaginationResponse<MyMatchingJoinRequestResponse>>> getMyJoinRequests(
            @Valid @ParameterObject @ModelAttribute MyMatchingJoinRequestFilter filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MyMatchingJoinRequestResponse> result =
                matchingGroupService.getMyJoinRequests(filter, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MY_MATCHING_JOIN_REQUESTS_FETCHED_SUCCESS
        ));
    }

    @Operation(
        summary = "Duyệt thành viên xin vào nhóm",
        description = "Cho phép Trưởng nhóm (Leader/Owner) phê duyệt yêu cầu tham gia nhóm ghép của thành viên đang ở trạng thái PENDING."
    )
    @PutMapping("/{groupId}/join-requests/{memberId}/approve")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> approveMember(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "UUID của bản ghi thành viên cần duyệt") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.approveMember(groupId, memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_APPROVED_SUCCESS));
    }

    @Operation(
        summary = "Từ chối thành viên xin vào nhóm",
        description = "Cho phép Trưởng nhóm (Leader/Owner) từ chối yêu cầu tham gia nhóm ghép của thành viên đang ở trạng thái PENDING."
    )
    @PutMapping("/{groupId}/join-requests/{memberId}/reject")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> rejectMember(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "UUID của bản ghi thành viên cần từ chối") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.rejectMember(groupId, memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_REJECTED_SUCCESS));
    }

    @Operation(
        summary = "Hủy yêu cầu tham gia nhóm ghép",
        description = "Cho phép Trekker hủy yêu cầu tham gia đang ở trạng thái PENDING và chuyển yêu cầu sang CANCELLED. " +
                "Thao tác này không thay đổi số thành viên hiện tại của nhóm."
    )
    @DeleteMapping("/{groupId}/join-request")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> cancelJoinRequest(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.cancelJoinRequest(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                result,
                MessageConstant.MATCHING_JOIN_REQUEST_CANCELLED_SUCCESS
        ));
    }

    @Operation(
        summary = "Rời khỏi nhóm ghép",
        description = "Cho phép thành viên ACCEPTED rời nhóm. Nhóm FULL chỉ mở lại khi vẫn còn hạn ghép, " +
                "ngày dự kiến đi chưa đến và Tour còn public."
    )
    @DeleteMapping("/{groupId}/members/me")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
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
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Void>> disbandMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        matchingGroupService.disbandMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.MATCHING_GROUP_DISBANDED_SUCCESS));
    }
}
