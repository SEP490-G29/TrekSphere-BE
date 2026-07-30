package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.SosAlertStatus;
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
    private UUID tourSessionId;
    private String tourName;
    private UUID senderId;
    private String senderName;
    private String senderRole;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String message;
    private SosAlertStatus status;
    private LocalDateTime createdAt;
    private UUID resolvedById;
    private String resolvedByName;
}
