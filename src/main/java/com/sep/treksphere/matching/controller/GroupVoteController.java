package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import com.sep.treksphere.matching.service.GroupVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/votes")
@RequiredArgsConstructor
@Tag(name = "Group Vote", description = "Bình chọn chung, bầu Trưởng nhóm và giải tán nhóm ghép")
public class GroupVoteController {

    private final GroupVoteService groupVoteService;

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Mở bình chọn chung mới (voteType = OTHER) trong nhóm")
    public ResponseEntity<ApiResponse<GroupVoteResponse>> createGeneralPoll(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupVoteResponse response = groupVoteService.createGeneralPoll(groupId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.GROUP_VOTE_CREATED_SUCCESS));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Lấy danh sách bình chọn của nhóm (phân trang, lọc theo voteType/status)")
    public ResponseEntity<ApiResponse<PaginationResponse<GroupVoteResponse>>> getVotes(
            @PathVariable UUID groupId,
            @RequestParam(required = false) VoteType voteType,
            @RequestParam(required = false) VoteStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Page<GroupVoteResponse> page = groupVoteService.getVotes(groupId, voteType, status, pageable, currentUserId);
        PaginationResponse<GroupVoteResponse> response = PaginationResponse.<GroupVoteResponse>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_VOTES_FETCHED));
    }

    @GetMapping("/{voteId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Lấy chi tiết 1 bình chọn")
    public ResponseEntity<ApiResponse<GroupVoteResponse>> getVoteDetail(
            @PathVariable UUID groupId,
            @PathVariable UUID voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupVoteResponse response = groupVoteService.getVoteDetail(groupId, voteId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @PostMapping("/{voteId}/ballots")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Bỏ phiếu cho 1 option (mỗi Member đúng 1 phiếu/vote)")
    public ResponseEntity<ApiResponse<GroupVoteResponse>> castBallot(
            @PathVariable UUID groupId,
            @PathVariable UUID voteId,
            @Valid @RequestBody CastBallotRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupVoteResponse response = groupVoteService.castBallot(groupId, voteId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_VOTE_BALLOT_CAST_SUCCESS));
    }

    @PostMapping("/{voteId}/close")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Đóng bình chọn khi đến hạn hoặc đã đủ phiếu (idempotent nếu đã đóng)")
    public ResponseEntity<ApiResponse<GroupVoteResponse>> closeVote(
            @PathVariable UUID groupId,
            @PathVariable UUID voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupVoteResponse response = groupVoteService.closeVote(groupId, voteId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_VOTE_CLOSED_SUCCESS));
    }

    @PostMapping("/{voteId}/cancel")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_VOTE')")
    @Operation(summary = "Huỷ bình chọn sớm (người mở vote hoặc Leader), không tính kết quả")
    public ResponseEntity<ApiResponse<GroupVoteResponse>> cancelVote(
            @PathVariable UUID groupId,
            @PathVariable UUID voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupVoteResponse response = groupVoteService.cancelVote(groupId, voteId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.GROUP_VOTE_CANCELLED_SUCCESS));
    }
}
