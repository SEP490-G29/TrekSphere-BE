package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostCommentResponse {

    private UUID groupPostCommentId;
    private UUID groupPostId;

    private UUID answeredByMatchingMemberId;
    private UUID answeredByUserId;
    private String answeredByFullName;
    private String answeredByAvatarUrl;
    private MatchingRole answeredByRole;

    private String content;
    private GroupContentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
