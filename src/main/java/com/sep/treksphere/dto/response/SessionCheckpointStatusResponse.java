package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCheckpointStatusResponse {

    private UUID sessionCheckpointLogId;
    private UUID tourSessionId;
    private UUID checkpointId;
    private String checkpointName;
    private String checkpointDescription;
    private Integer checkpointOrder;
    private BigDecimal checkpointLatitude;
    private BigDecimal checkpointLongitude;
    private BigDecimal checkpointAltitude;
    private String checkpointImageUrl;
    private SessionCheckpointLogStatus status;
    private String note;
}
