package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BlogFilterRequest;
import com.sep.treksphere.dto.request.CreateBlogRequest;
import com.sep.treksphere.dto.request.UpdateBlogRequest;
import com.sep.treksphere.dto.response.BlogDetailResponse;
import com.sep.treksphere.dto.response.BlogSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface BlogService {

    PaginationResponse<BlogSummaryResponse> getBlogs(BlogFilterRequest filter);

    BlogDetailResponse getBlogById(UUID blogId);

    BlogDetailResponse createBlog(CreateBlogRequest request, CustomUserDetails userDetails, MultipartFile coverImage);

    BlogDetailResponse updateBlog(UUID blogId, UpdateBlogRequest request, CustomUserDetails userDetails, MultipartFile coverImage);

    void hideBlog(UUID blogId, CustomUserDetails userDetails);

    void deleteBlog(UUID blogId, CustomUserDetails userDetails);
}
