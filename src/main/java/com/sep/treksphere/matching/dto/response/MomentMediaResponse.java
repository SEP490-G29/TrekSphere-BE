package com.sep.treksphere.matching.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomentMediaResponse {
    private UUID momentMediaId;
    private String imageUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
