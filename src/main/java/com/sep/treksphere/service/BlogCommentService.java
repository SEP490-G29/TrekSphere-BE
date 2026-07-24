package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CreateCommentRequest;
import com.sep.treksphere.dto.request.BlogCommentFilterRequest;
import com.sep.treksphere.dto.request.UpdateCommentRequest;
import com.sep.treksphere.dto.response.BlogCommentResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface BlogCommentService {

    /**
     * Lấy danh sách bình luận top-level có phân trang.
     * Mỗi bình luận top-level kèm theo tất cả replies (cây lồng nhau).
     */
    PaginationResponse<BlogCommentResponse> getCommentsByBlogId(UUID blogId, BlogCommentFilterRequest filter);

    BlogCommentResponse addComment(UUID blogId, CreateCommentRequest request, CustomUserDetails userDetails);

    BlogCommentResponse updateComment(UUID commentId, UpdateCommentRequest request, CustomUserDetails userDetails);

    void deleteComment(UUID commentId, CustomUserDetails userDetails);
}
