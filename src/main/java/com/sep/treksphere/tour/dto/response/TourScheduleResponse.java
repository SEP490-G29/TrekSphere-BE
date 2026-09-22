package com.sep.treksphere.tour.dto.response;

import com.sep.treksphere.tour.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourScheduleResponse {

    private String scheduleId;
    private String tourId;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private ScheduleStatus status;
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    // Audit fields
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
