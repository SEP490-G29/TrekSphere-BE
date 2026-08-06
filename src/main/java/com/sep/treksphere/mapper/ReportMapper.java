package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.report.ReportResponse;
import com.sep.treksphere.entity.ReportContent;
import com.sep.treksphere.enums.report.ReportTargetType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class ReportMapper {

    @Mapping(target = "id", source = "reportContentId")
    @Mapping(target = "reporterFullName", source = "reporter.fullName")
    @Mapping(target = "reporterEmail", source = "reporter.email")
    @Mapping(target = "resolvedByFullName", source = "resolvedBy.fullName")
    @Mapping(target = "targetType", expression = "java(determineTargetType(report))")
    @Mapping(target = "targetId", expression = "java(determineTargetId(report))")
    public abstract ReportResponse toReportResponse(ReportContent report);

    protected ReportTargetType determineTargetType(ReportContent report) {
        if (report.getBlog() != null) return ReportTargetType.BLOG;
        if (report.getBlogComment() != null) return ReportTargetType.COMMENT;
        if (report.getReview() != null) return ReportTargetType.REVIEW;
        return null;
    }

    protected UUID determineTargetId(ReportContent report) {
        if (report.getBlog() != null) return report.getBlog().getBlogId();
        if (report.getBlogComment() != null) return report.getBlogComment().getBlogCommentId();
        if (report.getReview() != null) return report.getReview().getReviewId();
        return null;
    }
}
