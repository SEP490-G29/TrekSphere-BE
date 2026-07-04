package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.blog.CommentStatus;
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
public class BlogCommentResponse {

    private String commentId;
    private String userId;
    private String userFullName;
    private String userAvatarUrl;
    private String content;
    private CommentStatus status;
    private LocalDateTime createdAt;

    private List<BlogCommentResponse> replies;
}
