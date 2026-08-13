package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import com.sep.treksphere.enums.tour.SosAlertStatus;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TrackingSnapshotResponse {
    private UUID sessionId;
    private TourSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long revision;
    private List<ParticipantState> participants;
    private List<EquipmentState> equipments;
    private List<CheckpointState> checkpoints;
    private SosState latestSos;

    @Data
    @Builder
    public static class ParticipantState {
        private UUID participantId;
        private String fullName;
        private Boolean isPresentStart;
        private LocalDateTime startAttendedAt;
        private Boolean isPresentEnd;
        private LocalDateTime endAttendedAt;
    }

    @Data
    @Builder
    public static class EquipmentState {
        private UUID sessionEquipmentId;
        private UUID equipmentId;
        private String equipmentName;
        private Integer quantity;
        private Boolean isChecked;
        private String note;
    }

    @Data
    @Builder
    public static class CheckpointState {
        private UUID checkpointId;
        private String checkpointName;
        private Integer checkpointOrder;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private SessionCheckpointLogStatus status;
        private LocalDateTime reachedAt;
        private String note;
    }

    @Data
    @Builder
    public static class SosState {
        private UUID sosAlertId;
        private SosAlertStatus status;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String message;
        private LocalDateTime createdAt;
    }
}
