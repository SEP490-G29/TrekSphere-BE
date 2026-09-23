package com.sep.treksphere.report.mapper;

import com.sep.treksphere.report.dto.response.ReportResponse;
import com.sep.treksphere.report.entity.ReportContent;
import com.sep.treksphere.report.enums.ReportTargetType;
import com.sep.treksphere.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class ReportMapper {

    @Mapping(target = "id", source = "reportContentId")
    @Mapping(target = "reporterFullName", source = "reporter.fullName")
    @Mapping(target = "reporterEmail", source = "reporter.email")
    @Mapping(target = "reporterAvatar", source = "reporter.avatarUrl")
    @Mapping(target = "resolvedByFullName", source = "resolvedBy.fullName")
    @Mapping(target = "targetType", expression = "java(determineTargetType(report))")
    @Mapping(target = "targetId", expression = "java(determineTargetId(report))")
    @Mapping(target = "targetTitle", expression = "java(determineTargetTitle(report))")
    @Mapping(target = "targetContent", expression = "java(determineTargetContent(report))")
    @Mapping(target = "targetAuthorId", expression = "java(determineTargetAuthorId(report))")
    @Mapping(target = "targetAuthorFullName", expression = "java(determineTargetAuthorFullName(report))")
    @Mapping(target = "targetAuthorEmail", expression = "java(determineTargetAuthorEmail(report))")
    @Mapping(target = "targetAuthorAvatar", expression = "java(determineTargetAuthorAvatar(report))")
    @Mapping(target = "targetAuthorStatus", expression = "java(determineTargetAuthorStatus(report))")
    @Mapping(target = "targetAuthorTrustScore", expression = "java(determineTargetAuthorTrustScore(report))")
    public abstract ReportResponse toReportResponse(ReportContent report);

    protected User extractAuthor(ReportContent report) {
        if (report == null) return null;
        if (report.getBlog() != null) return report.getBlog().getUser();
        if (report.getBlogComment() != null) return report.getBlogComment().getUser();
        if (report.getTour() != null) return report.getTour().getCreator();
        return null;
    }

    protected UUID determineTargetAuthorId(ReportContent report) {
        User author = extractAuthor(report);
        return author != null ? author.getUserId() : null;
    }

    protected String determineTargetAuthorFullName(ReportContent report) {
        User author = extractAuthor(report);
        return author != null ? author.getFullName() : null;
    }

    protected String determineTargetAuthorEmail(ReportContent report) {
        User author = extractAuthor(report);
        return author != null ? author.getEmail() : null;
    }

    protected String determineTargetAuthorAvatar(ReportContent report) {
        User author = extractAuthor(report);
        return author != null ? author.getAvatarUrl() : null;
    }

    protected String determineTargetAuthorStatus(ReportContent report) {
        User author = extractAuthor(report);
        return (author != null && author.getStatus() != null) ? author.getStatus().name() : null;
    }

    protected Short determineTargetAuthorTrustScore(ReportContent report) {
        User author = extractAuthor(report);
        return author != null ? author.getTrustScore() : null;
    }

    protected ReportTargetType determineTargetType(ReportContent report) {
        if (report.getBlog() != null) return ReportTargetType.BLOG;
        if (report.getBlogComment() != null) return ReportTargetType.COMMENT;
        if (report.getTour() != null) return ReportTargetType.TOUR;
        return null;
    }

    protected UUID determineTargetId(ReportContent report) {
        if (report.getBlog() != null) return report.getBlog().getBlogId();
        if (report.getBlogComment() != null) return report.getBlogComment().getBlogCommentId();
        if (report.getTour() != null) return report.getTour().getTourId();
        return null;
    }

    protected String determineTargetTitle(ReportContent report) {
        if (report.getBlog() != null) return report.getBlog().getTitle();
        if (report.getBlogComment() != null) return "Bình luận Blog";
        if (report.getTour() != null) return report.getTour().getTourName();
        return null;
    }

    protected String determineTargetContent(ReportContent report) {
        if (report.getBlog() != null) return report.getBlog().getContent();
        if (report.getBlogComment() != null) return report.getBlogComment().getContent();
        if (report.getTour() != null) return report.getTour().getDescription();
        return null;
    }
}
