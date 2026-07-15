package com.sep.treksphere.service.impl;

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
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BlogSummaryResponse> getBlogs(
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Blog> blogPage = blogRepository.searchBlogs(
                BlogStatus.PUBLISHED,
                normalizedKeyword,
                pageable);

        return PaginationUtils.toPaginationResponse(blogPage.map(this::toSummaryResponse));
    }

    @Override
    @Transactional
    public BlogDetailResponse getBlogById(UUID blogId) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setViewCount(blog.getViewCount() + 1);
        blogRepository.save(blog);
        List<BlogComment> allComments = blogCommentRepository
                .findAllByBlogIdAndStatus(blogId, CommentStatus.ACTIVE);

        List<BlogCommentResponse> commentTree = buildCommentTree(allComments);

        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.ACTIVE);

        return toDetailResponse(blog, commentTree, totalComments);
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
                .replies(new java.util.ArrayList<>())
                .build();
    }

    private Blog getBlogAndVerifyOwnership(UUID blogId, CustomUserDetails userDetails) {
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

    @Override
    @Transactional
    public void hideBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnership(blogId, userDetails);
        blog.setStatus(BlogStatus.HIDDEN);
        blogRepository.save(blog);
        log.info("User {} successfully hid blog {}. New status: {}", userDetails.getUser().getUserId(), blogId, blog.getStatus());
    }

    @Override
    @Transactional
    public void deleteBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnership(blogId, userDetails);
        blog.setStatus(BlogStatus.DELETED);
        blog.setIsDeleted(true);
        blog.setDeletedAt(java.time.LocalDateTime.now());
        blog.setDeletedBy(userDetails.getUser().getUserId().toString());
        blogRepository.save(blog);
        log.info("User {} successfully deleted blog {}. New status: {}, isDeleted: {}", userDetails.getUser().getUserId(), blogId, blog.getStatus(), blog.getIsDeleted());
    }

}
