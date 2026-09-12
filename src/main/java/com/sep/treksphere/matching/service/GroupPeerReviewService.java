package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.PeerReviewCreateRequest;
import com.sep.treksphere.matching.dto.response.PeerReviewCandidateResponse;
import com.sep.treksphere.matching.dto.response.PeerReviewResponse;

import java.util.List;
import java.util.UUID;

public interface GroupPeerReviewService {

    /**
     * Gửi đánh giá bạn đồng hành trong chuyến đi đã kết thúc.
     */
    PeerReviewResponse submitPeerReview(UUID groupId, UUID reviewerUserId, PeerReviewCreateRequest request);

    /**
     * Lấy danh sách bạn đồng hành trong chuyến đi để đánh giá, kèm trạng thái đã/chưa đánh giá.
     */
    List<PeerReviewCandidateResponse> getPeerReviewCandidates(UUID groupId, UUID currentUserId);

    /**
     * Lấy danh sách các đánh giá trong nhóm.
     */
    List<PeerReviewResponse> getGroupPeerReviews(UUID groupId, UUID currentUserId);

    /**
     * Lấy danh sách đánh giá nhận được của một người dùng (công khai ẩn danh).
     */
    List<PeerReviewResponse> getUserPeerReviews(UUID userId);
}
