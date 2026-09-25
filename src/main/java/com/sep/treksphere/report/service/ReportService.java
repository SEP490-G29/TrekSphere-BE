package com.sep.treksphere.report.service;

import com.sep.treksphere.blog.entity.Blog;
import com.sep.treksphere.blog.entity.BlogComment;
import com.sep.treksphere.blog.enums.BlogStatus;
import com.sep.treksphere.blog.enums.CommentStatus;
import com.sep.treksphere.blog.repository.BlogCommentRepository;
import com.sep.treksphere.blog.repository.BlogRepository;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.enums.ReferenceType;
import com.sep.treksphere.notification.service.NotificationService;
import com.sep.treksphere.report.dto.request.CreateReportRequest;
import com.sep.treksphere.report.dto.request.ReportFilterRequest;
import com.sep.treksphere.report.dto.request.ResolveReportRequest;
import com.sep.treksphere.report.dto.response.ReportResponse;
import com.sep.treksphere.report.entity.ReportContent;
import com.sep.treksphere.report.enums.ReportAction;
import com.sep.treksphere.report.enums.ReportStatus;
import com.sep.treksphere.report.enums.ReportTargetType;
import com.sep.treksphere.report.mapper.ReportMapper;
import com.sep.treksphere.report.repository.ReportContentRepository;
import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.repository.TourRepository;
import com.sep.treksphere.tour.service.TourService;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportContentRepository reportContentRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final TourRepository tourRepository;
    private final TourService tourService;
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

        User targetAuthor = extractTargetAuthor(report);
        if (targetAuthor != null && targetAuthor.getUserId().equals(reporterId)) {
            throw new AppException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
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

        User author = extractTargetAuthor(report);

        int defaultPenalty = switch (action) {
            case WARNING -> 5;
            case HIDE_CONTENT -> 10;
            case DISMISS -> 0;
        };
        int penalty = request.getPenaltyTrustScore() != null ? request.getPenaltyTrustScore() : defaultPenalty;
        if (author != null && penalty > 0 && action != ReportAction.DISMISS) {
            short currentScore = author.getTrustScore() != null ? author.getTrustScore() : 100;
            short newScore = (short) Math.max(0, currentScore - penalty);
            author.setTrustScore(newScore);
            userRepository.save(author);
            log.info("Deducted {} trust points from author {}. New trust score: {}", penalty, author.getUserId(), newScore);
        }

        switch (action) {
            case WARNING -> {
                if (author != null) {
                    String targetTitle = describeReportTargetTitle(report);
                    String penaltyMsg = penalty > 0 ? " Bạn đã bị trừ " + penalty + " điểm tín nhiệm." : "";
                    String actionUrl = resolveReportActionUrl(report);
                    notificationService.notify(
                            author.getUserId(),
                            NotificationEventType.REPORT_WARNING_ISSUED,
                            ReferenceType.REPORT, report.getReportContentId(), actionUrl,
                            targetTitle, contentOwnerReason, penaltyMsg);
                }
                report.setStatus(ReportStatus.RESOLVED);
            }
            case HIDE_CONTENT -> {
                hideTargetContent(report, admin, contentOwnerReason);
                report.setStatus(ReportStatus.RESOLVED);
            }
            case DISMISS -> {
                report.setStatus(ReportStatus.REJECTED);
            }
        }

        report.setResolutionNotes(request.getResolutionNotes());
        report.setResolvedBy(admin);

        reportContentRepository.save(report);
        log.info("Admin {} resolved report {} with action {}", adminId, reportId, action);

        String reporterActionUrl = (action == ReportAction.HIDE_CONTENT)
                ? null
                : resolveReportActionUrl(report);

        notificationService.notify(
                report.getReporter().getUserId(),
                NotificationEventType.REPORT_RESOLVED,
                ReferenceType.REPORT, report.getReportContentId(), reporterActionUrl,
                describeResolution(action));
    }

    private void hideTargetContent(ReportContent report, User admin, String reason) {
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
            tourService.hideTourForViolation(admin.getUserId(), tour.getTourId(), reason);
        }
    }

    private User extractTargetAuthor(ReportContent report) {
        if (report == null) return null;
        if (report.getBlog() != null) return report.getBlog().getUser();
        if (report.getBlogComment() != null) return report.getBlogComment().getUser();
        if (report.getTour() != null) return report.getTour().getCreator();
        return null;
    }

    private String describeReportTargetTitle(ReportContent report) {
        if (report == null) return "Nội dung";
        if (report.getBlog() != null) return report.getBlog().getTitle();
        if (report.getBlogComment() != null) return "Bình luận Blog";
        if (report.getTour() != null) return report.getTour().getTourName();
        return "Nội dung";
    }

    private String describeResolution(ReportAction action) {
        return switch (action) {
            case HIDE_CONTENT -> "nội dung đã bị ẩn";
            case WARNING -> "đã gửi cảnh báo tới người vi phạm";
            case DISMISS -> "báo cáo không hợp lệ, đã được từ chối";
        };
    }

    private String resolveReportActionUrl(ReportContent report) {
        if (report == null) return null;
        if (report.getBlog() != null) {
            return "/news/" + report.getBlog().getBlogId();
        }
        if (report.getBlogComment() != null) {
            UUID blogId = report.getBlogComment().getBlog() != null ? report.getBlogComment().getBlog().getBlogId() : null;
            if (blogId != null) {
                return "/news/" + blogId + "#comment-" + report.getBlogComment().getBlogCommentId();
            }
        }
        if (report.getTour() != null) {
            return "/tours/" + report.getTour().getTourId();
        }
        return null;
    }
}
