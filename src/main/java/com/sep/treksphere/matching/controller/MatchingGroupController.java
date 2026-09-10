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
import com.sep.treksphere.matching.dto.request.MatchingGroupUpdateRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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
        summary = "Chỉnh sửa thông tin nhóm ghép bạn đồng hành",
        description = "Cho phép Trưởng nhóm (Leader) chỉnh sửa thông tin nhóm (tên, mô tả, sức chứa, ngày dự kiến, hạn chót) " +
                "và thông tin hành trình Custom Journey (nếu chưa bị khóa). Không cho phép giảm sức chứa nhỏ hơn số thành viên đang tham gia."
    )
    @PatchMapping("/{groupId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> updateMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody MatchingGroupUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.updateMatchingGroup(groupId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_UPDATED_SUCCESS));
    }

    @Operation(
        summary = "Ẩn nhóm ghép khỏi danh sách tìm kiếm công khai",
        description = "Cho phép Trưởng nhóm (Leader) tạm ẩn nhóm ghép khỏi kết quả tìm kiếm (chuyển sang trạng thái HIDDEN)."
    )
    @PostMapping("/{groupId}/hide")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> hideMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.hideMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_HIDDEN_SUCCESS));
    }

    @Operation(
        summary = "Hiển thị lại nhóm ghép ra công khai",
        description = "Cho phép Trưởng nhóm (Leader) hiển thị lại nhóm từ trạng thái HIDDEN. Hệ thống sẽ tự động tính toán lại trạng thái (OPEN, FULL hoặc CLOSED) dựa theo số lượng thành viên, hạn chót và ngày đi."
    )
    @PostMapping("/{groupId}/show")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> showMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.showMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_SHOWN_SUCCESS));
    }

    @Operation(
        summary = "Đóng tuyển thành viên nhóm ghép",
        description = "Cho phép Trưởng nhóm (Leader) chủ động đóng tuyển thành viên mới (chuyển sang trạng thái CLOSED)."
    )
    @PostMapping("/{groupId}/close")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> closeMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.closeMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_CLOSED_SUCCESS));
    }

    @Operation(
        summary = "Mở lại tuyển thành viên nhóm ghép",
        description = "Cho phép Trưởng nhóm (Leader) mở lại tuyển thành viên từ trạng thái CLOSED nếu hạn chót và ngày đi còn hợp lệ."
    )
    @PostMapping("/{groupId}/open")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingGroupDetailResponse>> openMatchingGroup(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingGroupDetailResponse result = matchingGroupService.openMatchingGroup(groupId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_GROUP_OPENED_SUCCESS));
    }

    @Operation(
        summary = "Gửi đơn xin gia nhập nhóm ghép bạn đồng hành",
        description = "Cho phép Trekker nộp đơn xin gia nhập vào một nhóm ghép đang mở (OPEN) và còn chỗ. " +
                "Tạo bản ghi matching_member với role=MEMBER, status=PENDING và phát event GroupApplicationSubmitted."
    )
    @PostMapping(value = {"/{groupId}/applications", "/{groupId}/join"})
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> submitApplication(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody(required = false) com.sep.treksphere.matching.dto.request.GroupApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.submitApplication(groupId, request, userDetails);
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
    @GetMapping(value = {"/{groupId}/applications", "/{groupId}/join-requests"})
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
        summary = "Xem danh sách các đơn xin tham gia nhóm ghép của tôi",
        description = "Cho phép Trekker xem toàn bộ các đơn xin tham gia nhóm ghép của chính mình. " +
                "Hỗ trợ lọc theo trạng thái đơn (JoinStatus: PENDING, ACCEPTED, REJECTED, WITHDRAWN, LEFT, REMOVED) và phân trang."
    )
    @GetMapping(value = {"/my-applications", "/applications/me", "/join-requests/me"})
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
        summary = "Duyệt đơn xin gia nhập nhóm ghép",
        description = "Cho phép Trưởng nhóm (Leader) phê duyệt đơn xin gia nhập nhóm ghép của thành viên đang ở trạng thái PENDING. " +
                "Cập nhật trạng thái thành viên sang ACCEPTED và tăng current_size của nhóm. Tự động chuyển trạng thái nhóm sang FULL nếu đã đủ sĩ số."
    )
    @PostMapping(value = {"/{groupId}/applications/{memberId}/approve", "/{groupId}/join-requests/{memberId}/approve"})
    @PutMapping(value = {"/{groupId}/applications/{memberId}/approve", "/{groupId}/join-requests/{memberId}/approve"})
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> approveMember(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "UUID của bản ghi thành viên cần duyệt") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.approveMember(groupId, memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_APPROVED_SUCCESS));
    }

    @Operation(
        summary = "Từ chối đơn xin gia nhập nhóm ghép",
        description = "Cho phép Trưởng nhóm (Leader) từ chối đơn xin gia nhập nhóm ghép của thành viên đang ở trạng thái PENDING. " +
                "Chuyển trạng thái sang REJECTED và không thay đổi sĩ số hiện tại của nhóm."
    )
    @PostMapping(value = {"/{groupId}/applications/{memberId}/reject", "/{groupId}/join-requests/{memberId}/reject"})
    @PutMapping(value = {"/{groupId}/applications/{memberId}/reject", "/{groupId}/join-requests/{memberId}/reject"})
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> rejectMember(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "UUID của bản ghi thành viên cần từ chối") @PathVariable UUID memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.rejectMember(groupId, memberId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.MATCHING_MEMBER_REJECTED_SUCCESS));
    }

    @Operation(
        summary = "Rút đơn xin tham gia nhóm ghép",
        description = "Cho phép Trekker rút đơn xin tham gia đang ở trạng thái PENDING và chuyển trạng thái sang WITHDRAWN. " +
                "Ghi nhận thời điểm withdrawn_at và không thay đổi số thành viên hiện tại của nhóm."
    )
    @PostMapping(value = {"/{groupId}/applications/me/withdraw", "/{groupId}/applications/withdraw"})
    @DeleteMapping("/{groupId}/join-request")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    public ResponseEntity<ApiResponse<MatchingMemberResponse>> withdrawApplication(
            @Parameter(description = "UUID của nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchingMemberResponse result = matchingGroupService.withdrawApplication(groupId, userDetails);
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
