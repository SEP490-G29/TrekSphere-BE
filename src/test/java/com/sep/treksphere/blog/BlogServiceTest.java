package com.sep.treksphere.blog;

import com.sep.treksphere.blog.comment.BlogCommentRepository;
import com.sep.treksphere.blog.comment.CommentStatus;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chỉ bao phủ đúng 1 method mà `unit_test_gap_report.md` liệt kê thiếu: `BlogService.getBlogs`.
 * 5 method còn lại (createBlog/getBlogById/updateBlog/hideBlog/deleteBlog) coi như đã có test ở
 * nơi khác, không viết lại ở đây.
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private BlogCommentRepository blogCommentRepository;
    @Mock
    private FileService fileService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BlogService blogService;

    private Blog publishedBlog;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setUserId(UUID.randomUUID());
        author.setFullName("Nguyen Van Author");
        author.setAvatarUrl("https://example.com/avatar.jpg");

        publishedBlog = new Blog();
        publishedBlog.setBlogId(UUID.randomUUID());
        publishedBlog.setUser(author);
        publishedBlog.setTitle("Hành trình Fansipan");
        publishedBlog.setCoverImageUrl("https://example.com/cover.jpg");
        publishedBlog.setStatus(BlogStatus.PUBLISHED);
        publishedBlog.setViewCount(10);
    }

    @Test
    @DisplayName("getBlogs: có keyword + authorId -> trim rồi truyền xuống repository, chỉ lấy blog PUBLISHED")
    void getBlogs_WithKeywordAndAuthorId_DelegatesTrimmedFilterToRepository() {
        BlogFilterRequest filter = new BlogFilterRequest();
        filter.setKeyword("  fansipan  ");
        filter.setAuthorId(" " + publishedBlog.getUser().getUserId() + " ");

        Page<Blog> page = new PageImpl<>(List.of(publishedBlog));
        when(blogRepository.searchBlogs(eq(BlogStatus.PUBLISHED), eq("fansipan"),
                eq(publishedBlog.getUser().getUserId().toString()), any(Pageable.class)))
                .thenReturn(page);
        when(blogCommentRepository.countByBlogAndStatus(publishedBlog, CommentStatus.VISIBLE)).thenReturn(3);

        PaginationResponse<BlogSummaryResponse> result = blogService.getBlogs(filter);

        assertThat(result.getContent()).hasSize(1);
        BlogSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getBlogId()).isEqualTo(publishedBlog.getBlogId().toString());
        assertThat(summary.getTitle()).isEqualTo("Hành trình Fansipan");
        assertThat(summary.getStatus()).isEqualTo(BlogStatus.PUBLISHED);
        assertThat(summary.getTotalComments()).isEqualTo(3);
        assertThat(summary.getAuthorName()).isEqualTo("Nguyen Van Author");

        verify(blogRepository).searchBlogs(eq(BlogStatus.PUBLISHED), eq("fansipan"),
                eq(publishedBlog.getUser().getUserId().toString()), any(Pageable.class));
    }

    @Test
    @DisplayName("getBlogs: keyword/authorId rỗng hoặc toàn khoảng trắng -> coi như null, không lọc")
    void getBlogs_BlankKeywordAndAuthorId_TreatedAsNull() {
        BlogFilterRequest filter = new BlogFilterRequest();
        filter.setKeyword("   ");
        filter.setAuthorId("");

        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> authorIdCaptor = ArgumentCaptor.forClass(String.class);
        when(blogRepository.searchBlogs(eq(BlogStatus.PUBLISHED), keywordCaptor.capture(),
                authorIdCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PaginationResponse<BlogSummaryResponse> result = blogService.getBlogs(filter);

        assertThat(result.getContent()).isEmpty();
        assertThat(keywordCaptor.getValue()).isNull();
        assertThat(authorIdCaptor.getValue()).isNull();
    }
}
