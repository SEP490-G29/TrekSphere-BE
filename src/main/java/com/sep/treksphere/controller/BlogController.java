package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BlogCommentFilterRequest;
import com.sep.treksphere.dto.request.BlogFilterRequest;
import com.sep.treksphere.dto.request.CreateBlogRequest;
import com.sep.treksphere.dto.request.CreateCommentRequest;
import com.sep.treksphere.dto.request.UpdateBlogRequest;
import com.sep.treksphere.dto.request.UpdateCommentRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BlogCommentResponse;
import com.sep.treksphere.dto.response.BlogDetailResponse;
import com.sep.treksphere.dto.response.BlogSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BlogCommentService;
import com.sep.treksphere.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Các API dành cho Blog")
public class BlogController {

    private final BlogService blogService;
    private final BlogCommentService blogCommentService;

    // ======================== BLOG ========================

    @Operation(
        summary = "Lấy danh sách bài viết blog",
        description = "Lấy danh sách bài viết blog chia sẻ (public). Hỗ trợ tìm kiếm theo từ khóa, lọc theo tác giả, phân trang và sắp xếp."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<BlogSummaryResponse>>> getBlogs(
            @Valid @ParameterObject @ModelAttribute BlogFilterRequest filter) {
        PaginationResponse<BlogSummaryResponse> result = blogService.getBlogs(filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(
        summary = "Xem chi tiết bài viết blog",
        description = "Xem chi tiết nội dung bài viết blog (public). Tăng viewCount mỗi lần gọi."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogDetailResponse>> getBlogById(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, blogService.getBlogById(id)));
    }

    @Operation(
        summary = "Đăng bài viết blog mới",
        description = "Trekker hoặc VendorStaff đăng bài viết blog mới. Blog được đăng với trạng thái PUBLISHED."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TREKKER', 'VENDOR_STAFF', 'VENDOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<BlogDetailResponse>> createBlog(
            @Valid @RequestBody CreateBlogRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BlogDetailResponse result = blogService.createBlog(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.BLOG_CREATED_SUCCESSFULLY));
    }

    @Operation(
        summary = "Chỉnh sửa bài viết blog",
        description = "Tác giả chỉnh sửa nội dung bài viết blog của mình. Chỉ chủ bài viết mới có quyền."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogDetailResponse>> updateBlog(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID id,
            @Valid @RequestBody UpdateBlogRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BlogDetailResponse result = blogService.updateBlog(id, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.BLOG_UPDATED_SUCCESSFULLY));
    }

    @Operation(
        summary = "Ẩn bài viết blog",
        description = "Tác giả ẩn blog của chính mình, hoặc Admin ẩn blog vi phạm."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hideBlog(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        blogService.hideBlog(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.BLOG_HIDDEN_SUCCESSFULLY));
    }

    @Operation(
        summary = "Xóa bài viết blog",
        description = "Tác giả xóa blog của chính mình, hoặc Admin xóa blog vi phạm."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        blogService.deleteBlog(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.BLOG_DELETED_SUCCESSFULLY));
    }

    // ======================== COMMENT ========================

    @Operation(
        summary = "Xem danh sách bình luận",
        description = "Lấy danh sách bình luận top-level của bài viết blog có phân trang. Mỗi bình luận kèm theo replies (cây lồng nhau). Public, không cần đăng nhập."
    )
    @GetMapping("/{blogId}/comments")
    public ResponseEntity<ApiResponse<PaginationResponse<BlogCommentResponse>>> getComments(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID blogId,
            @Valid @ParameterObject @ModelAttribute BlogCommentFilterRequest filter) {
        PaginationResponse<BlogCommentResponse> result = blogCommentService.getCommentsByBlogId(blogId, filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(
        summary = "Gửi bình luận",
        description = "Gửi bình luận mới hoặc trả lời bình luận khác (cung cấp parentCommentId). Yêu cầu đăng nhập."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{blogId}/comments")
    public ResponseEntity<ApiResponse<BlogCommentResponse>> addComment(
            @Parameter(description = "UUID của bài viết") @PathVariable UUID blogId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BlogCommentResponse result = blogCommentService.addComment(blogId, request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.COMMENT_ADDED_SUCCESSFULLY));
    }

    @Operation(
        summary = "Sửa nội dung bình luận",
        description = "Người dùng sửa bình luận của chính mình. Chỉ chủ bình luận mới có quyền."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<BlogCommentResponse>> updateComment(
            @Parameter(description = "UUID của bình luận") @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BlogCommentResponse result = blogCommentService.updateComment(commentId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.COMMENT_UPDATED_SUCCESSFULLY));
    }

    @Operation(
        summary = "Xóa bình luận",
        description = "Người dùng xóa bình luận của chính mình. Admin cũng có thể xóa."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "UUID của bình luận") @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        blogCommentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.COMMENT_DELETED_SUCCESSFULLY));
    }
}
