package com.sep.treksphere.report.dto.response;

import com.sep.treksphere.report.enums.ReportStatus;
import com.sep.treksphere.report.enums.ReportTargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ReportResponse {
    
    private UUID id;
    
    private ReportTargetType targetType;
    
    private UUID targetId;
    
    private String reason;
    
    private ReportStatus status;
    
    private String reporterFullName;
    
    private String reporterEmail;
    
    private String reporterAvatar;
    
    private String targetTitle;
    
    private String targetContent;

    private UUID targetAuthorId;

    private String targetAuthorFullName;

    private String targetAuthorEmail;

    private String targetAuthorAvatar;

    private String targetAuthorStatus;

    private Short targetAuthorTrustScore;
    
    private String resolutionNotes;
    
    private String resolvedByFullName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
