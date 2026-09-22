package com.sep.treksphere.blog.service;

import com.sep.treksphere.blog.dto.request.BlogFilterRequest;
import com.sep.treksphere.blog.dto.request.CreateBlogRequest;
import com.sep.treksphere.blog.dto.request.UpdateBlogRequest;
import com.sep.treksphere.blog.dto.response.BlogCommentResponse;
import com.sep.treksphere.blog.dto.response.BlogDetailResponse;
import com.sep.treksphere.blog.dto.response.BlogSummaryResponse;
import com.sep.treksphere.blog.entity.Blog;
import com.sep.treksphere.blog.entity.BlogComment;
import com.sep.treksphere.blog.enums.BlogStatus;
import com.sep.treksphere.blog.enums.CommentStatus;
import com.sep.treksphere.blog.repository.BlogCommentRepository;
import com.sep.treksphere.blog.repository.BlogRepository;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.file.service.FileService;
import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.enums.ReferenceType;
import com.sep.treksphere.notification.service.NotificationService;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.enums.UserStatus;
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
public class BlogService {

    public static final String SYSTEM_USER_ANONYMOUS_NAME = "NGƯỜI DÙNG HỆ THỐNG";

    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final FileService fileService;
    private final NotificationService notificationService;

    private static final String TREKKER_BLOG_LIST_URL = "/trekker/blog";

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
                .findAllByBlogIdAndStatus(blogId, CommentStatus.VISIBLE);
        List<BlogCommentResponse> commentTree = buildCommentTree(allComments);
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.VISIBLE);

        return toDetailResponse(blog, commentTree, totalComments);
    }

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
                .findAllByBlogIdAndStatus(blogId, CommentStatus.VISIBLE);
        List<BlogCommentResponse> commentTree = buildCommentTree(allComments);
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.VISIBLE);

        return toDetailResponse(blog, commentTree, totalComments);
    }

    @Transactional
    public void hideBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnershipOrAdmin(blogId, userDetails);
        blog.setStatus(BlogStatus.HIDDEN);
        blogRepository.save(blog);
        log.info("User {} hid blog {}. New status: {}", userDetails.getUser().getUserId(), blogId, blog.getStatus());

        if (isAdminModerationAction(blog, userDetails)) {
            notificationService.notify(
                    blog.getUser().getUserId(),
                    NotificationEventType.BLOG_HIDDEN,
                    ReferenceType.BLOG, blog.getBlogId(), TREKKER_BLOG_LIST_URL,
                    blog.getTitle());
        }
    }

    @Transactional
    public void deleteBlog(UUID blogId, CustomUserDetails userDetails) {
        Blog blog = getBlogAndVerifyOwnershipOrAdmin(blogId, userDetails);
        boolean isModeration = isAdminModerationAction(blog, userDetails);
        String blogTitle = blog.getTitle();

        blog.setStatus(BlogStatus.DELETED);
        blog.setIsDeleted(true);
        blog.setDeletedAt(LocalDateTime.now());
        blog.setDeletedBy(userDetails.getUser().getUserId().toString());
        blogRepository.save(blog);
        log.info("User {} deleted blog {}", userDetails.getUser().getUserId(), blogId);

        if (isModeration) {
            notificationService.notify(
                    blog.getUser().getUserId(),
                    NotificationEventType.BLOG_DELETED,
                    ReferenceType.BLOG, blog.getBlogId(), TREKKER_BLOG_LIST_URL,
                    "Bài viết \"" + blogTitle + "\" của bạn đã bị xoá bởi quản trị viên.");
        }
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

    private boolean isAdminModerationAction(Blog blog, CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isAuthor = blog.getUser().getUserId().equals(userDetails.getUser().getUserId());
        return isAdmin && !isAuthor;
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

    private boolean isUserLocked(User user) {
        return user != null && user.getStatus() == UserStatus.LOCKED;
    }

    private BlogSummaryResponse toSummaryResponse(Blog blog) {
        int totalComments = blogCommentRepository.countByBlogAndStatus(blog, CommentStatus.VISIBLE);
        boolean isLocked = isUserLocked(blog.getUser());
        return BlogSummaryResponse.builder()
                .blogId(blog.getBlogId().toString())
                .title(blog.getTitle())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .authorId(isLocked ? null : blog.getUser().getUserId().toString())
                .authorName(isLocked ? SYSTEM_USER_ANONYMOUS_NAME : blog.getUser().getFullName())
                .authorAvatarUrl(isLocked ? null : blog.getUser().getAvatarUrl())
                .totalComments(totalComments)
                .createdAt(blog.getCreatedAt())
                .build();
    }

    private BlogDetailResponse toDetailResponse(Blog blog,
            List<BlogCommentResponse> comments,
            int totalComments) {
        boolean isLocked = isUserLocked(blog.getUser());
        return BlogDetailResponse.builder()
                .blogId(blog.getBlogId().toString())
                .title(blog.getTitle())
                .content(blog.getContent())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .authorId(isLocked ? null : blog.getUser().getUserId().toString())
                .authorName(isLocked ? SYSTEM_USER_ANONYMOUS_NAME : blog.getUser().getFullName())
                .authorAvatarUrl(isLocked ? null : blog.getUser().getAvatarUrl())
                .comments(comments)
                .totalComments(totalComments)
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }

    private BlogCommentResponse toCommentResponse(BlogComment comment) {
        boolean isLocked = isUserLocked(comment.getUser());
        return BlogCommentResponse.builder()
                .commentId(comment.getBlogCommentId().toString())
                .userId(isLocked ? null : comment.getUser().getUserId().toString())
                .userFullName(isLocked ? SYSTEM_USER_ANONYMOUS_NAME : comment.getUser().getFullName())
                .userAvatarUrl(isLocked ? null : comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
}
