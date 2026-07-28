package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantAttendanceItem {

    @NotNull(message = MessageConstant.PARTICIPANT_ID_REQUIRED)
    private UUID participantId;

    @NotNull(message = MessageConstant.ATTENDANCE_STATUS_REQUIRED)
    private Boolean isPresent;
}
