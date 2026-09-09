package com.sep.treksphere.matching.service;

import java.util.UUID;

public interface TrustScoreService {

    /**
     * Tính users.trust_score theo công thức Bayesian ở db_refactor_v3.md Mục 8.9.
     * trust_score/trust_review_count/trust_calculated_at là cache/projection — source of truth
     * luôn là group_peer_review, nên hàm này luôn tính lại từ đầu (không cộng dồn incremental)
     * để đảm bảo tái tạo đúng dù review bị xoá hoặc đổi moderation_status.
     */
    void recalculateTrustScore(UUID userId);
}
