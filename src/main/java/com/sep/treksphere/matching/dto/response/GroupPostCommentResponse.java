package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostCommentResponse {

    private UUID groupPostCommentId;
    private UUID groupPostId;

    private UUID parentCommentId;
    private UUID replyToCommentId;
    private UUID replyToUserId;
    private String replyToFullName;

    private UUID answeredByMatchingMemberId;
    private UUID answeredByUserId;
    private String answeredByFullName;
    private String answeredByAvatarUrl;
    private MatchingRole answeredByRole;

    private String content;
    private GroupContentStatus status;

    @Builder.Default
    private List<GroupPostCommentResponse> replies = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

