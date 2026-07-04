package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.blog.BlogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlogSummaryResponse {

    private String blogId;
    private String title;
    private String coverImageUrl;
    private BlogStatus status;
    private Integer viewCount;

    private String authorId;
    private String authorName;
    private String authorAvatarUrl;

    private int totalComments;
    private LocalDateTime createdAt;
}
