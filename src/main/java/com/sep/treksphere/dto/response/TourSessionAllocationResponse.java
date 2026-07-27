package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.TourSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourSessionAllocationResponse {
    private UUID sessionId;
    private TourSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
    private UUID tourId;
    private String tourName;
    private LocalDate departureDate;
    private LocalDate returnDate;
    
    private List<CoordinatorAllocationDto> coordinators;
}
