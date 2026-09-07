package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CustomJourneyCreateRequest {

    @NotBlank(message = MessageConstant.CUSTOM_JOURNEY_TITLE_REQUIRED)
    @Size(max = 200, message = MessageConstant.CUSTOM_JOURNEY_TITLE_MAX_LENGTH)
    private String title;

    @Size(max = 2000, message = MessageConstant.CUSTOM_JOURNEY_DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotNull(message = MessageConstant.CUSTOM_JOURNEY_DIFFICULTY_REQUIRED)
    private JourneyDifficulty difficulty;

    @NotNull(message = MessageConstant.CUSTOM_JOURNEY_START_DATE_REQUIRED)
    private LocalDate startDate;

    @NotNull(message = MessageConstant.CUSTOM_JOURNEY_END_DATE_REQUIRED)
    private LocalDate endDate;
}
