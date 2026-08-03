package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.tour.AttendanceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = MessageConstant.ATTENDANCE_LIST_TOO_LARGE)
    @Valid
    private List<ParticipantAttendanceItem> participants;
}
