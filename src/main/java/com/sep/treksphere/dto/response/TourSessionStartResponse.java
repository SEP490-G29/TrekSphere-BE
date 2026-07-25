package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.TourSessionStatus;
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
public class TourSessionStartResponse {

    private UUID tourSessionId;
    private TourSessionStatus status;
    private LocalDateTime startedAt;
}
