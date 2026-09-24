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
    private UUID responderId;
    private String responderName;
    private String responderPhone;
    private String responderAvatarUrl;
    private LocalDateTime respondedAt;
    private UUID resolvedById;
    private String resolvedByName;
    private String senderEmergencyContactName;
    private String senderEmergencyContactPhone;
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
                .senderEmergencyContactName(alert.getSender().getEmergencyContactName())
                .senderEmergencyContactPhone(alert.getSender().getEmergencyContactPhone())
                .incidentTypeCode(alert.getIncidentTypeCode())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .responderId(alert.getResponder() != null ? alert.getResponder().getUserId() : null)
                .responderName(alert.getResponder() != null ? alert.getResponder().getFullName() : null)
                .responderPhone(alert.getResponder() != null ? alert.getResponder().getPhone() : null)
                .responderAvatarUrl(alert.getResponder() != null ? alert.getResponder().getAvatarUrl() : null)
                .respondedAt(alert.getRespondedAt())
                .resolvedById(alert.getResolvedBy() != null ? alert.getResolvedBy().getUserId() : null)
                .resolvedByName(alert.getResolvedBy() != null ? alert.getResolvedBy().getFullName() : null)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
