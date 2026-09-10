package com.sep.treksphere.report;

import com.sep.treksphere.blog.Blog;
import com.sep.treksphere.blog.comment.BlogComment;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.user.User;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.blog.comment.BlogCommentRepository;
import com.sep.treksphere.blog.BlogRepository;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.blog.BlogStatus;
import com.sep.treksphere.blog.comment.CommentStatus;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.common.util.PaginationUtils;
import org.springframework.data.domain.Page;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportContentRepository reportContentRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final TourRepository tourRepository;
    private final ReportMapper reportMapper;
    private final NotificationService notificationService;

    @Transactional
    public void createReport(CreateReportRequest request, UUID reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ReportContent report = new ReportContent();
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setStatus(ReportStatus.PENDING);

        switch (request.getTargetType()) {
            case BLOG:
                Blog blog = blogRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new AppException(ErrorCode.REPORT_TARGET_NOT_FOUND));
                report.setBlog(blog);
                break;
            case COMMENT:
                BlogComment comment = blogCommentRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new AppException(ErrorCode.REPORT_TARGET_NOT_FOUND));
                report.setBlogComment(comment);
                break;
            case TOUR:
                Tour tour = tourRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new AppException(ErrorCode.REPORT_TARGET_NOT_FOUND));
                report.setTour(tour);
                break;
            default:
                throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        reportContentRepository.save(report);
        log.info("User {} created a report for {} with ID {}", reporterId, request.getTargetType(), request.getTargetId());

        List<UUID> adminIds = userRepository.findDistinctByRoles_RoleNameAndIsDeletedFalse("ADMIN").stream()
                .map(User::getUserId)
                .toList();

        notificationService.notify(
                adminIds,
                NotificationEventType.REPORT_SUBMITTED,
                ReferenceType.REPORT, report.getReportContentId(),
                "/admin/reports/" + report.getReportContentId(),
                reporter.getFullName(), describeTargetType(request.getTargetType()));
    }

    private String describeTargetType(ReportTargetType targetType) {
        return switch (targetType) {
            case BLOG -> "một bài viết";
            case COMMENT -> "một bình luận";
            case TOUR -> "một tour";
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ReportResponse> getReportsForAdmin(ReportFilterRequest filter) {
        Page<ReportContent> reportPage = reportContentRepository.findByStatusAndIsDeletedFalse(filter.getStatus(), filter.getPageable());
        return PaginationUtils.toPaginationResponse(reportPage.map(reportMapper::toReportResponse));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportByIdForAdmin(UUID reportId) {
        ReportContent report = reportContentRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        return reportMapper.toReportResponse(report);
    }

    @Transactional
    public void resolveReport(UUID reportId, ResolveReportRequest request, UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ReportContent report = reportContentRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }

        ReportAction action = request.getAction();
        String contentOwnerReason = StringUtils.hasText(request.getResolutionNotes())
                ? request.getResolutionNotes()
                : report.getReason();

        if (action == ReportAction.HIDE_CONTENT) {
            if (report.getBlog() != null) {
                Blog blog = report.getBlog();
                blog.setStatus(BlogStatus.HIDDEN);
                blogRepository.save(blog);
                notificationService.notify(
                        blog.getUser().getUserId(),
                        NotificationEventType.BLOG_HIDDEN,
                        ReferenceType.BLOG, blog.getBlogId(), "/trekker/blog",
                        blog.getTitle());
            } else if (report.getBlogComment() != null) {
                BlogComment comment = report.getBlogComment();
                comment.setStatus(CommentStatus.HIDDEN);
                blogCommentRepository.save(comment);
            } else if (report.getTour() != null) {
                Tour tour = report.getTour();
                tour.setStatus(TourStatus.HIDDEN);
                tour.setHiddenReason(request.getResolutionNotes());
                tour.setHiddenAt(java.time.LocalDateTime.now());
                tour.setHiddenBy(admin);
                tourRepository.save(tour);
                notificationService.notify(
                        tour.getVendor().getManager().getUserId(),
                        NotificationEventType.TOUR_HIDDEN_VIOLATION,
                        ReferenceType.TOUR, tour.getTourId(),
                        "/vendor/tours/" + tour.getTourId(),
                        tour.getTourName(), contentOwnerReason);
            }
        }

        if (action == ReportAction.DISMISS) {
            report.setStatus(ReportStatus.REJECTED);
        } else {
            report.setStatus(ReportStatus.RESOLVED);
        }

        report.setResolutionNotes(request.getResolutionNotes());
        report.setResolvedBy(admin);

        reportContentRepository.save(report);
        log.info("Admin {} resolved report {} with action {}", adminId, reportId, action);

        notificationService.notify(
                report.getReporter().getUserId(),
                NotificationEventType.REPORT_RESOLVED,
                ReferenceType.REPORT, report.getReportContentId(), null,
                describeResolution(action));
    }

    private String describeResolution(ReportAction action) {
        return switch (action) {
            case HIDE_CONTENT -> "nội dung đã bị ẩn";
            case WARNING -> "đã gửi cảnh báo tới người vi phạm";
            case DISMISS -> "báo cáo không hợp lệ, đã được từ chối";
        };
    }
}
