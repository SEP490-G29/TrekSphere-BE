package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.AttendanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourSessionAttendanceResponse {

    private UUID tourSessionId;
    private AttendanceType attendanceType;
    private LocalDateTime recordedAt;
    private List<ParticipantAttendanceResponseItem> participants;
}
