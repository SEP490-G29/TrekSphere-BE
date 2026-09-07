package com.sep.treksphere.matching.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingGroupCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("[P2-S3] Missing Custom Journey difficulty có validation message ổn định")
    void validateCustomJourneyWithoutDifficulty_ReturnsStableViolation() {
        CustomJourneyCreateRequest journey = new CustomJourneyCreateRequest();
        MatchingGroupCreateRequest request = new MatchingGroupCreateRequest();
        request.setSourceType(MatchingGroupSourceType.CUSTOM_JOURNEY);
        request.setCustomJourney(journey);

        Set<ConstraintViolation<MatchingGroupCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("customJourney.difficulty");
            assertThat(violation.getMessage()).isEqualTo(MessageConstant.CUSTOM_JOURNEY_DIFFICULTY_REQUIRED);
        });
    }

    @Test
    @DisplayName("[P2-S3] Difficulty ngoài catalog bị từ chối khi deserialize request")
    void deserializeCustomJourneyWithInvalidDifficulty_IsRejected() {
        String json = """
                {
                  "sourceType": "CUSTOM_JOURNEY",
                  "customJourney": {
                    "difficulty": "EXPERT"
                  }
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, MatchingGroupCreateRequest.class))
                .hasMessageContaining("JourneyDifficulty")
                .hasMessageContaining("EXPERT");
    }
}
