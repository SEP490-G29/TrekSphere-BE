package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.report.CreateReportRequest;
import com.sep.treksphere.entity.Blog;
import com.sep.treksphere.entity.BlogComment;
import com.sep.treksphere.entity.ReportContent;
import com.sep.treksphere.entity.Review;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.report.ReportStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.ReportMapper;
import com.sep.treksphere.repository.BlogCommentRepository;
import com.sep.treksphere.repository.BlogRepository;
import com.sep.treksphere.repository.ReportContentRepository;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.sep.treksphere.dto.request.report.ReportFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.report.ReportResponse;
import com.sep.treksphere.enums.report.ReportTargetType;
import com.sep.treksphere.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportContentRepository reportContentRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final ReviewRepository reviewRepository;
    private final ReportMapper reportMapper;

    @Override
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
            case REVIEW:
                Review review = reviewRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new AppException(ErrorCode.REPORT_TARGET_NOT_FOUND));
                report.setReview(review);
                break;
            default:
                throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        reportContentRepository.save(report);
        log.info("User {} created a report for {} with ID {}", reporterId, request.getTargetType(), request.getTargetId());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ReportResponse> getReportsForAdmin(ReportFilterRequest filter) {
        Page<ReportContent> reportPage = reportContentRepository.findByStatusAndIsDeletedFalse(filter.getStatus(), filter.getPageable());
        return PaginationUtils.toPaginationResponse(reportPage.map(reportMapper::toReportResponse));
    }
}
