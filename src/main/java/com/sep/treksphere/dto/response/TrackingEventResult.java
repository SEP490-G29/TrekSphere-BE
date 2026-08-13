package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TrackingEventResult {
    private UUID clientEventId;
    private Long sequenceNumber;
    private String status;
    private String code;
    private String message;
    private String resourceType;
    private UUID resourceId;
    private Long resultRevision;
}
