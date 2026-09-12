package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.PeerReviewCreateRequest;
import com.sep.treksphere.matching.dto.response.PeerReviewCandidateResponse;
import com.sep.treksphere.matching.dto.response.PeerReviewResponse;
import com.sep.treksphere.matching.service.GroupPeerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/peer-reviews")
@RequiredArgsConstructor
@Tag(name = "Group Peer Reviews", description = "Quản lý đánh giá bạn đồng hành sau khi kết thúc chuyến đi")
public class GroupPeerReviewController {

    private final GroupPeerReviewService groupPeerReviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gửi đánh giá cho một thành viên trong đoàn (chỉ khi chuyến đi đã ENDED)")
    public ResponseEntity<ApiResponse<PeerReviewResponse>> submitPeerReview(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeerReviewCreateRequest request
    ) {
        PeerReviewResponse response = groupPeerReviewService.submitPeerReview(
                groupId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.PEER_REVIEW_SUBMITTED_SUCCESS));
    }

    @GetMapping("/candidates")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách thành viên cần đánh giá trong chuyến đi kèm trạng thái đã/chưa đánh giá")
    public ResponseEntity<ApiResponse<List<PeerReviewCandidateResponse>>> getPeerReviewCandidates(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<PeerReviewCandidateResponse> candidates = groupPeerReviewService.getPeerReviewCandidates(
                groupId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, candidates, MessageConstant.PENDING_PEER_REVIEWS_FETCHED_SUCCESS));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy toàn bộ danh sách đánh giá trong nhóm")
    public ResponseEntity<ApiResponse<List<PeerReviewResponse>>> getGroupPeerReviews(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<PeerReviewResponse> reviews = groupPeerReviewService.getGroupPeerReviews(
                groupId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, reviews, MessageConstant.PEER_REVIEWS_FETCHED_SUCCESS));
    }
}
