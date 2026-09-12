package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.MomentVisibility;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomentCreateRequest {

    @Size(max = 2000, message = "Chú thích không được vượt quá 2000 ký tự")
    private String caption;

    private LocalDateTime capturedAt;

    @Size(max = 200, message = "Tên địa điểm không được vượt quá 200 ký tự")
    private String placeName;

    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng -90 đến 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng -180 đến 180")
    private BigDecimal longitude;

    private MomentVisibility visibility;

    @NotEmpty(message = "Khoảnh khắc phải có ít nhất một hình ảnh")
    @Size(max = 20, message = "Khoảnh khắc không được vượt quá 20 hình ảnh")
    private List<String> mediaUrls;
}
