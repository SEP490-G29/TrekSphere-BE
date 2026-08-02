package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BlogCommentFilterRequest;
import com.sep.treksphere.dto.request.CreateCommentRequest;
import com.sep.treksphere.dto.request.UpdateCommentRequest;
import com.sep.treksphere.dto.response.BlogCommentResponse;
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
import com.sep.treksphere.service.BlogCommentService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BlogCommentResponse> getCommentsByBlogId(UUID blogId, BlogCommentFilterRequest filter) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        // Lấy top-level comments có phân trang
        Page<BlogComment> topLevelPage = blogCommentRepository
                .findTopLevelByBlogId(blogId, CommentStatus.ACTIVE, filter.getPageable());

        // Với mỗi top-level comment, load replies (cây lồng nhau)
        Page<BlogCommentResponse> responsePage = topLevelPage.map(comment -> {
            BlogCommentResponse response = toCommentResponse(comment);
            List<BlogComment> replies = blogCommentRepository
                    .findRepliesByParentId(comment.getBlogCommentId(), CommentStatus.ACTIVE);
            response.setReplies(buildReplyTree(replies));
            return response;
        });

        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional
    public BlogCommentResponse addComment(UUID blogId, CreateCommentRequest request, CustomUserDetails userDetails) {
        Blog blog = blogRepository.findDetailById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        BlogComment comment = new BlogComment();
        comment.setBlog(blog);
        comment.setUser(userDetails.getUser());
        comment.setContent(request.getContent());
        comment.setStatus(CommentStatus.ACTIVE);

        if (request.getParentCommentId() != null) {
            BlogComment parentComment = blogCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND));

            if (parentComment.getStatus() != CommentStatus.ACTIVE 
                    || Boolean.TRUE.equals(parentComment.getIsDeleted())
                    || !parentComment.getBlog().getBlogId().equals(blogId)) {
                throw new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND);
            }
            comment.setParentComment(parentComment);
        }

        blogCommentRepository.save(comment);
        log.info("User {} added comment to blog {}", userDetails.getUser().getUserId(), blogId);

        return toCommentResponse(comment);
    }

    @Override
    @Transactional
    public BlogCommentResponse updateComment(UUID commentId, UpdateCommentRequest request, CustomUserDetails userDetails) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND));

        if (comment.getStatus() != CommentStatus.ACTIVE || Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND);
        }

        boolean isOwner = comment.getUser().getUserId().equals(userDetails.getUser().getUserId());
        if (!isOwner) {
            log.warn("User {} attempted to edit comment {} without permission", userDetails.getUser().getUserId(), commentId);
            throw new AppException(ErrorCode.COMMENT_CANNOT_EDIT);
        }

        comment.setContent(request.getContent());
        blogCommentRepository.save(comment);
        log.info("User {} updated comment {}", userDetails.getUser().getUserId(), commentId);

        return toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, CustomUserDetails userDetails) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND));

        if (comment.getStatus() != CommentStatus.ACTIVE || Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new AppException(ErrorCode.BLOG_COMMENT_NOT_FOUND);
        }

        boolean isOwner = comment.getUser().getUserId().equals(userDetails.getUser().getUserId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            log.warn("User {} attempted to delete comment {} without permission", userDetails.getUser().getUserId(), commentId);
            throw new AppException(ErrorCode.COMMENT_CANNOT_DELETE);
        }

        comment.setStatus(CommentStatus.DELETED);
        comment.setIsDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        comment.setDeletedBy(userDetails.getUser().getUserId().toString());
        blogCommentRepository.save(comment);
        log.info("User {} deleted comment {}", userDetails.getUser().getUserId(), commentId);
    }

    // ===================== Helpers =====================

    /**
     * Build reply tree cho một top-level comment.
     * Load tất cả replies phẳng, sau đó lồng nhau theo parentComment.
     */
    private List<BlogCommentResponse> buildReplyTree(List<BlogComment> replies) {
        if (replies == null || replies.isEmpty()) return new ArrayList<>();

        Map<UUID, BlogCommentResponse> responseMap = replies.stream()
                .collect(Collectors.toMap(
                        BlogComment::getBlogCommentId,
                        this::toCommentResponse,
                        (a, b) -> a));

        for (BlogComment reply : replies) {
            if (reply.getParentComment() != null) {
                UUID parentId = reply.getParentComment().getBlogCommentId();
                BlogCommentResponse parent = responseMap.get(parentId);
                if (parent != null) {
                    parent.getReplies().add(responseMap.get(reply.getBlogCommentId()));
                }
            }
        }

        // Trả về chỉ những reply trực tiếp của top-level (parentComment là top-level comment)
        return replies.stream()
                .map(r -> responseMap.get(r.getBlogCommentId()))
                .toList();
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
