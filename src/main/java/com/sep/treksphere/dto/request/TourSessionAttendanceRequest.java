package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.tour.AttendanceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourSessionAttendanceRequest {

    @NotNull(message = MessageConstant.ATTENDANCE_TYPE_REQUIRED)
    private AttendanceType attendanceType;

    @NotEmpty(message = MessageConstant.ATTENDANCE_LIST_REQUIRED)
    @Valid
    private List<ParticipantAttendanceItem> participants;
}
