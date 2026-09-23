package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.entity.SosAlert;
import com.sep.treksphere.matching.enums.IncidentType;
import com.sep.treksphere.matching.enums.SosAlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosAlertResponse {

    private UUID sosAlertId;
    private UUID groupTripId;
    private UUID matchingGroupId;
    private UUID senderId;
    private String senderName;
    private String senderPhone;
    private String senderAvatarUrl;
    private IncidentType incidentTypeCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String message;
    private SosAlertStatus status;
    private UUID resolvedById;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SosAlertResponse from(SosAlert alert) {
        return SosAlertResponse.builder()
                .sosAlertId(alert.getSosAlertId())
                .groupTripId(alert.getGroupTrip().getGroupTripId())
                .matchingGroupId(alert.getGroupTrip().getMatchingGroup().getMatchingGroupId())
                .senderId(alert.getSender().getUserId())
                .senderName(alert.getSender().getFullName())
                .senderPhone(alert.getSender().getPhone())
                .senderAvatarUrl(alert.getSender().getAvatarUrl())
                .incidentTypeCode(alert.getIncidentTypeCode())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .resolvedById(alert.getResolvedBy() != null ? alert.getResolvedBy().getUserId() : null)
                .resolvedByName(alert.getResolvedBy() != null ? alert.getResolvedBy().getFullName() : null)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
