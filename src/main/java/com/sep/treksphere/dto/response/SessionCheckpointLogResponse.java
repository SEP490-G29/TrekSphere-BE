package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCheckpointLogResponse {

    private UUID sessionCheckpointLogId;
    private UUID checkpointId;
    private String checkpointName;
    private Integer checkpointOrder;
    private SessionCheckpointLogStatus status;
    private LocalDateTime reachedAt;
}
