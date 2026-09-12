package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomentResponse {
    private UUID momentId;
    private UUID authorUserId;
    private String authorName;
    private String authorAvatarUrl;
    private UUID matchingGroupId;
    private String groupName;
    private UUID authorMatchingMemberId;
    private String caption;
    private LocalDateTime capturedAt;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private MomentVisibility visibility;
    private MomentStatus status;
    private UUID hiddenByUserId;
    private String hiddenReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MomentMediaResponse> mediaList;
}
