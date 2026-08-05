package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupCreateRequest {

    @NotNull(message = MessageConstant.MATCHING_TOUR_ID_REQUIRED)
    private UUID tourId;

    @NotBlank(message = MessageConstant.MATCHING_GROUP_NAME_REQUIRED)
    @Size(min = 3, max = 100, message = MessageConstant.MATCHING_GROUP_NAME_SIZE)
    private String groupName;

    @Size(max = 2000, message = MessageConstant.MATCHING_GROUP_DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotNull(message = MessageConstant.MATCHING_GROUP_MAX_SIZE_REQUIRED)
    @Min(value = 2, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MIN)
    @Max(value = 100, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MAX)
    private Integer maxSize;

    @NotNull(message = MessageConstant.MATCHING_TARGET_DATE_REQUIRED)
    private LocalDate targetDate;

    @NotNull(message = MessageConstant.MATCHING_DEADLINE_REQUIRED)
    private LocalDateTime matchingDeadline;
}
