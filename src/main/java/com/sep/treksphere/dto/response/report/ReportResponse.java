package com.sep.treksphere.dto.response.report;

import com.sep.treksphere.enums.report.ReportStatus;
import com.sep.treksphere.enums.report.ReportTargetType;
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
    
    private String resolutionNotes;
    
    private String resolvedByFullName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
