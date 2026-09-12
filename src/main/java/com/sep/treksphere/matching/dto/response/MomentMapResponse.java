package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomentMapResponse {
    private UUID momentId;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String thumbnailUrl;
    private LocalDateTime capturedAt;
    private String authorName;
    private String authorAvatarUrl;
    private UUID matchingGroupId;
    private String groupName;
}
