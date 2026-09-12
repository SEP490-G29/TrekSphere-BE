package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerReviewCandidateResponse {

    private UUID matchingMemberId;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private MatchingRole role;
    private String roleLabel;
    private boolean isReviewed;
    private UUID existingReviewId;
}
