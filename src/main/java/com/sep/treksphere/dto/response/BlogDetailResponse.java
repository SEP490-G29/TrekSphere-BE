package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.blog.BlogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlogDetailResponse {

    private String blogId;
    private String title;
    private String content;
    private String coverImageUrl;
    private BlogStatus status;
    private Integer viewCount;

    private String authorId;
    private String authorName;
    private String authorAvatarUrl;

    private List<BlogCommentResponse> comments;
    private int totalComments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
