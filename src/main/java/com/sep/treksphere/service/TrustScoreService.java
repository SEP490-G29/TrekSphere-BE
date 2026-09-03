package com.sep.treksphere.service;

import java.util.UUID;

public interface TrustScoreService {

    /**
     * Tính lại users.trust_score / trust_review_count / trust_calculated_at của một user
     * từ toàn bộ group_peer_review đang VISIBLE mà user đó là reviewee.
     * Gọi lại mỗi khi có review mới được tạo, hoặc khi moderation_status của một review đổi.
     */
    void recalculateTrustScore(UUID userId);
}
