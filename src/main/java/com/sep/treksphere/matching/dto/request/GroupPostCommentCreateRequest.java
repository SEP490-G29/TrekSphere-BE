package com.sep.treksphere.matching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostCommentCreateRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;

    private UUID replyToCommentId;
}

