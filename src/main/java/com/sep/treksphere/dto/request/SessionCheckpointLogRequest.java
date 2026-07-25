package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCheckpointLogRequest {

    @NotNull(message = MessageConstant.LATITUDE_REQUIRED)
    @DecimalMin(value = ValidationConstant.MIN_LATITUDE, message = MessageConstant.LATITUDE_OUT_OF_BOUNDS)
    @DecimalMax(value = ValidationConstant.MAX_LATITUDE, message = MessageConstant.LATITUDE_OUT_OF_BOUNDS)
    private BigDecimal latitude;

    @NotNull(message = MessageConstant.LONGITUDE_REQUIRED)
    @DecimalMin(value = ValidationConstant.MIN_LONGITUDE, message = MessageConstant.LONGITUDE_OUT_OF_BOUNDS)
    @DecimalMax(value = ValidationConstant.MAX_LONGITUDE, message = MessageConstant.LONGITUDE_OUT_OF_BOUNDS)
    private BigDecimal longitude;

    private String note;
}
