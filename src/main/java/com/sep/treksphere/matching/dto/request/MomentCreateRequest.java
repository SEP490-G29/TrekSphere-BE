package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.MomentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request tạo khoảnh khắc hành trình")
public class MomentCreateRequest {

    @Size(max = 2000, message = MessageConstant.MOMENT_CAPTION_MAX_LENGTH)
    @Schema(description = "Chú thích hoặc nhật ký ngắn", example = "Chạm đỉnh Tà Xùa lúc bình minh rực rỡ")
    private String caption;

    @Schema(description = "Thời điểm chụp ảnh/video")
    private LocalDateTime capturedAt;

    @Size(max = 200, message = MessageConstant.MOMENT_LOCATION_NAME_MAX_LENGTH)
    @Schema(description = "Tên địa điểm chụp", example = "Sống lưng khủng long Háng Đồng")
    private String placeName;

    @DecimalMin(value = "-90.0", message = MessageConstant.MOMENT_LATITUDE_RANGE)
    @DecimalMax(value = "90.0", message = MessageConstant.MOMENT_LATITUDE_RANGE)
    @Schema(description = "Vĩ độ địa lý", example = "21.3524")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = MessageConstant.MOMENT_LONGITUDE_RANGE)
    @DecimalMax(value = "180.0", message = MessageConstant.MOMENT_LONGITUDE_RANGE)
    @Schema(description = "Kinh độ địa lý", example = "104.3125")
    private BigDecimal longitude;

    @Schema(description = "Quyền hiển thị (GROUP_ONLY, PUBLIC_PROFILE, ONLY_ME)", example = "PUBLIC_PROFILE")
    private MomentVisibility visibility;

    @NotEmpty(message = MessageConstant.MOMENT_MEDIA_REQUIRED)
    @Size(max = 20, message = MessageConstant.MOMENT_MEDIA_MAX_SIZE)
    @Schema(description = "Danh sách URL hình ảnh/video của khoảnh khắc")
    private List<String> mediaUrls;
}
