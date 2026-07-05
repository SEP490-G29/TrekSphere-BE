package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourScheduleResponse {

    private String scheduleId;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer availableSlots;
    private Integer bookedSlots;
    private BigDecimal price;
    private ScheduleStatus status;
}
