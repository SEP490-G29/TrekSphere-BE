package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.constant.ValidationConstant;
import com.sep.treksphere.matching.enums.IncidentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSosAlertRequest {

    @NotNull(message = MessageConstant.SOS_INCIDENT_TYPE_REQUIRED)
    private IncidentType incidentTypeCode;

    @DecimalMin(value = ValidationConstant.MIN_LATITUDE, message = MessageConstant.LATITUDE_OUT_OF_BOUNDS)
    @DecimalMax(value = ValidationConstant.MAX_LATITUDE, message = MessageConstant.LATITUDE_OUT_OF_BOUNDS)
    private BigDecimal latitude;

    @DecimalMin(value = ValidationConstant.MIN_LONGITUDE, message = MessageConstant.LONGITUDE_OUT_OF_BOUNDS)
    @DecimalMax(value = ValidationConstant.MAX_LONGITUDE, message = MessageConstant.LONGITUDE_OUT_OF_BOUNDS)
    private BigDecimal longitude;

    @Size(max = 2000, message = MessageConstant.SOS_MESSAGE_TOO_LONG)
    private String message;

    @NotBlank(message = MessageConstant.SOS_IDEMPOTENCY_KEY_REQUIRED)
    @Size(max = 255, message = MessageConstant.SOS_IDEMPOTENCY_KEY_TOO_LONG)
    private String idempotencyKey;
}
