package com.sep.treksphere.report;

import com.sep.treksphere.blog.Blog;
import com.sep.treksphere.blog.BlogRepository;
import com.sep.treksphere.blog.BlogStatus;
import com.sep.treksphere.blog.comment.BlogComment;
import com.sep.treksphere.blog.comment.BlogCommentRepository;
import com.sep.treksphere.blog.comment.CommentStatus;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportContentRepository reportContentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private BlogCommentRepository blogCommentRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourService tourService;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportService reportService;

    private User admin;
    private User reporter;
    private User author;
    private Blog blog;
    private ReportContent report;
    private UUID adminId;
    private UUID reportId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        reportId = UUID.randomUUID();

        admin = new User();
        admin.setUserId(adminId);
        admin.setFullName("Admin User");

        reporter = new User();
        reporter.setUserId(UUID.randomUUID());
        reporter.setFullName("Reporter User");

        author = new User();
        author.setUserId(UUID.randomUUID());
        author.setFullName("Author User");
        author.setTrustScore((short) 100);
        author.setStatus(UserStatus.ACTIVE);

        blog = new Blog();
        blog.setBlogId(UUID.randomUUID());
        blog.setTitle("Sample Blog");
        blog.setUser(author);
        blog.setStatus(BlogStatus.PUBLISHED);

        report = new ReportContent();
        report.setReportContentId(reportId);
        report.setReporter(reporter);
        report.setBlog(blog);
        report.setReason("Spam content");
        report.setStatus(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("resolveReport - Action WARNING should deduct trust score and notify author")
    void resolveReport_whenWarning_shouldDeductTrustScoreAndNotifyAuthor() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportContentRepository.findById(reportId)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest();
        request.setAction(ReportAction.WARNING);
        request.setResolutionNotes("Cảnh cáo lần 1");
        request.setPenaltyTrustScore(5);

        reportService.resolveReport(reportId, request, adminId);

        assertThat(author.getTrustScore()).isEqualTo((short) 95);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getResolvedBy()).isEqualTo(admin);

        verify(userRepository).save(author);
        verify(notificationService).notify(
                eq(author.getUserId()),
                eq(NotificationEventType.REPORT_WARNING_ISSUED),
                eq(ReferenceType.REPORT),
                eq(reportId),
                eq("/news/" + blog.getBlogId()),
                eq("Sample Blog"),
                eq("Cảnh cáo lần 1"),
                contains("5 điểm tín nhiệm")
        );
        verify(notificationService).notify(
                eq(reporter.getUserId()),
                eq(NotificationEventType.REPORT_RESOLVED),
                eq(ReferenceType.REPORT),
                eq(reportId),
                eq("/news/" + blog.getBlogId()),
                contains("cảnh báo")
        );
    }

    @Test
    @DisplayName("resolveReport - Action HIDE_CONTENT should hide blog and deduct 10 points")
    void resolveReport_whenHideContent_shouldHideBlogAndDeductTrustScore() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportContentRepository.findById(reportId)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest();
        request.setAction(ReportAction.HIDE_CONTENT);
        request.setResolutionNotes("Nội dung phản cảm");

        reportService.resolveReport(reportId, request, adminId);

        assertThat(author.getTrustScore()).isEqualTo((short) 90);
        assertThat(blog.getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);

        verify(blogRepository).save(blog);
        verify(userRepository).save(author);
        verify(notificationService).notify(
                eq(author.getUserId()),
                eq(NotificationEventType.BLOG_HIDDEN),
                eq(ReferenceType.BLOG),
                eq(blog.getBlogId()),
                eq("/trekker/blog"),
                eq("Sample Blog")
        );
    }

    @Test
    @DisplayName("resolveReport - Action DISMISS should reject report without penalizing user")
    void resolveReport_whenDismiss_shouldRejectReportWithoutPenalty() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportContentRepository.findById(reportId)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest();
        request.setAction(ReportAction.DISMISS);
        request.setResolutionNotes("Báo cáo không có căn cứ");

        reportService.resolveReport(reportId, request, adminId);

        assertThat(author.getTrustScore()).isEqualTo((short) 100);
        assertThat(author.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);

        verify(userRepository, never()).save(author);
        verify(blogRepository, never()).save(blog);
    }

    @Test
    @DisplayName("resolveReport - when already resolved should throw REPORT_ALREADY_RESOLVED")
    void resolveReport_whenAlreadyResolved_shouldThrowException() {
        report.setStatus(ReportStatus.RESOLVED);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportContentRepository.findById(reportId)).thenReturn(Optional.of(report));

        ResolveReportRequest request = new ResolveReportRequest();
        request.setAction(ReportAction.WARNING);

        assertThatThrownBy(() -> reportService.resolveReport(reportId, request, adminId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_ALREADY_RESOLVED);
    }
}
