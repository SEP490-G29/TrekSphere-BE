package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BlogDetailResponse;
import com.sep.treksphere.dto.response.BlogSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.service.BlogService;
import com.sep.treksphere.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Các API dành cho Blog (public - không cần đăng nhập)")
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<BlogSummaryResponse>>> getBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationResponse<BlogSummaryResponse> result = blogService.getBlogs(keyword, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogDetailResponse>> getBlogById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, blogService.getBlogById(id)));
    }

    @Operation(summary = "Ẩn Blog", description = "Tác giả ẩn blog của chính mình, hoặc Admin ẩn blog vi phạm.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hideBlog(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        blogService.hideBlog(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null));
    }

    @Operation(summary = "Xóa Blog", description = "Tác giả xóa blog của chính mình, hoặc Admin xóa blog vi phạm.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        blogService.deleteBlog(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null));
    }
}
