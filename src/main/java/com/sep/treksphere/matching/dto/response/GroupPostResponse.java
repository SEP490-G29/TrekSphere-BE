package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostResponse {

    private UUID groupPostId;
    private UUID matchingGroupId;

    private UUID postedByMatchingMemberId;
    private UUID postedByUserId;
    private String postedByFullName;
    private String postedByAvatarUrl;
    private MatchingRole postedByRole;

    private String title;
    private String content;
    private List<String> imageUrls;
    private GroupContentStatus status;
    private long commentCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
