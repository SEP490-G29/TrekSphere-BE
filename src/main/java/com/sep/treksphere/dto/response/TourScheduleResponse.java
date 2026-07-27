package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private Integer availableSlots;
    private Integer bookedSlots;
    private BigDecimal price;
    private ScheduleStatus status;

    // Audit fields
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
