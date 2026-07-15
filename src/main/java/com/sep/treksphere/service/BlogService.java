package com.sep.treksphere.service;

import com.sep.treksphere.dto.response.BlogDetailResponse;
import com.sep.treksphere.dto.response.BlogSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface BlogService {

    PaginationResponse<BlogSummaryResponse> getBlogs(String keyword, int page, int size, String sortBy, String sortDir);

    BlogDetailResponse getBlogById(UUID blogId);

    void hideBlog(UUID blogId, CustomUserDetails userDetails);

    void deleteBlog(UUID blogId, CustomUserDetails userDetails);
}
