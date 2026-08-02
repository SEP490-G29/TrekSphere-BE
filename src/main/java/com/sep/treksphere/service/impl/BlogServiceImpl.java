package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BlogFilterRequest;
import com.sep.treksphere.dto.request.CreateBlogRequest;
import com.sep.treksphere.dto.request.UpdateBlogRequest;
import com.sep.treksphere.dto.response.BlogCommentResponse;
import com.sep.treksphere.dto.response.BlogDetailResponse;
import com.sep.treksphere.dto.response.BlogSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.Blog;
import com.sep.treksphere.entity.BlogComment;
import com.sep.treksphere.enums.blog.BlogStatus;
import com.sep.treksphere.enums.blog.CommentStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BlogCommentRepository;
import com.sep.treksphere.repository.BlogRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BlogService;
import com.sep.treksphere.service.FileService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final FileService fileService;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BlogSummaryResponse> getBlogs(BlogFilterRequest filter) {
        String keyword = StringUtils.hasText(filter.getKeyword()) ? filter.getKeyword().trim() : null;
        String authorId = StringUtils.hasText(filter.getAuthorId()) ? filter.getAuthorId().trim() : null;

        Page<Blog> blogPage = blogRepository.searchBlogs(
                BlogStatus.PUBLISHED,
                keyword,
                authorId,
                filter.getPageable());

        return PaginationUtils.toPaginationResponse(blogPage.map(this::toSummaryResponse));
    }

    @Override
    @Transactional
    public BlogDetailResponse getBlogById(UUID blogId) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        blog.setViewCount(blog.getViewCount() + 1);
        blogRepository.save(blog);

        List<BlogComment> allComments = blogCommentRepository
                .findAllByBlogIdAndStatus(blogId, CommentStatus.ACTIVE);
        List<BlogCommentResponse> commentTree = buildCommentTree(allComments);
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.ACTIVE);

        return toDetailResponse(blog, commentTree, totalComments);
    }

    @Override
    @Transactional
    public BlogDetailResponse createBlog(CreateBlogRequest request, CustomUserDetails userDetails, MultipartFile coverImage) {
        Blog blog = new Blog();
        blog.setUser(userDetails.getUser());
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setStatus(BlogStatus.PUBLISHED);
        blog.setViewCount(0);

        // Upload cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "blogs");
            blog.setCoverImageUrl(coverUrl);
        }

        blogRepository.save(blog);
        log.info("User {} created blog '{}'", userDetails.getUser().getUserId(), request.getTitle());

        return toDetailResponse(blog, List.of(), 0);
    }

    @Override
    @Transactional
    public BlogDetailResponse updateBlog(UUID blogId, UpdateBlogRequest request, CustomUserDetails userDetails, MultipartFile coverImage) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_CANNOT_EDIT);
        }

        boolean isAuthor = blog.getUser().getUserId().equals(userDetails.getUser().getUserId());
        if (!isAuthor) {
            log.warn("User {} attempted to update blog {} without permission", userDetails.getUser().getUserId(), blogId);
            throw new AppException(ErrorCode.BLOG_CANNOT_EDIT);
        }

        if (StringUtils.hasText(request.getTitle())) {
            blog.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getContent())) {
            blog.setContent(request.getContent());
        }

        // Upload cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "blogs");
            blog.setCoverImageUrl(coverUrl);
        }

        blogRepository.save(blog);
        log.info("User {} updated blog {}", userDetails.getUser().getUserId(), blogId);

        List<BlogComment> allComments = blogCommentRepository
                .findAllByBlogIdAndStatus(blogId, CommentStatus.ACTIVE);
        List<BlogCommentResponse> commentTree = buildCommentTree(allComments);
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.ACTIVE);

        return toDetailResponse(blog, commentTree, totalComments);
    }

    @Override
    @Transactional
    public void hideBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnershipOrAdmin(blogId, userDetails);
        blog.setStatus(BlogStatus.HIDDEN);
        blogRepository.save(blog);
        log.info("User {} hid blog {}. New status: {}", userDetails.getUser().getUserId(), blogId, blog.getStatus());
    }

    @Override
    @Transactional
    public void deleteBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnershipOrAdmin(blogId, userDetails);
        blog.setStatus(BlogStatus.DELETED);
        blog.setIsDeleted(true);
        blog.setDeletedAt(LocalDateTime.now());
        blog.setDeletedBy(userDetails.getUser().getUserId().toString());
        blogRepository.save(blog);
        log.info("User {} deleted blog {}", userDetails.getUser().getUserId(), blogId);
    }

    // ===================== Helpers =====================

    private Blog getBlogAndVerifyOwnershipOrAdmin(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isAuthor = blog.getUser().getUserId().equals(userDetails.getUser().getUserId());

        if (!isAdmin && !isAuthor) {
            log.warn("User {} attempted to modify blog {} without permission", userDetails.getUser().getUserId(), blogId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return blog;
    }

    private List<BlogCommentResponse> buildCommentTree(List<BlogComment> allComments) {
        Map<UUID, BlogCommentResponse> responseMap = allComments.stream()
                .collect(Collectors.toMap(
                        BlogComment::getBlogCommentId,
                        this::toCommentResponse,
                        (a, b) -> a));

        for (BlogComment comment : allComments) {
            if (comment.getParentComment() != null) {
                UUID parentId = comment.getParentComment().getBlogCommentId();
                BlogCommentResponse parent = responseMap.get(parentId);
                if (parent != null) {
                    parent.getReplies().add(responseMap.get(comment.getBlogCommentId()));
                }
            }
        }

        return allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(c -> responseMap.get(c.getBlogCommentId()))
                .toList();
    }

    private BlogSummaryResponse toSummaryResponse(Blog blog) {
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.ACTIVE);
        return BlogSummaryResponse.builder()
                .blogId(blog.getBlogId().toString())
                .title(blog.getTitle())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .authorId(blog.getUser().getUserId().toString())
                .authorName(blog.getUser().getFullName())
                .authorAvatarUrl(blog.getUser().getAvatarUrl())
                .totalComments(totalComments)
                .createdAt(blog.getCreatedAt())
                .build();
    }

    private BlogDetailResponse toDetailResponse(Blog blog,
            List<BlogCommentResponse> comments,
            int totalComments) {
        return BlogDetailResponse.builder()
                .blogId(blog.getBlogId().toString())
                .title(blog.getTitle())
                .content(blog.getContent())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .authorId(blog.getUser().getUserId().toString())
                .authorName(blog.getUser().getFullName())
                .authorAvatarUrl(blog.getUser().getAvatarUrl())
                .comments(comments)
                .totalComments(totalComments)
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }

    private BlogCommentResponse toCommentResponse(BlogComment comment) {
        return BlogCommentResponse.builder()
                .commentId(comment.getBlogCommentId().toString())
                .userId(comment.getUser().getUserId().toString())
                .userFullName(comment.getUser().getFullName())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
}
