package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateTourRequest {

    @NotBlank(message = MessageConstant.TOUR_NAME_REQUIRED)
    @Schema(description = "Tên tour", example = "Tour leo núi Fansipan 2 ngày 1 đêm")
    private String tourName;

    @NotBlank(message = MessageConstant.TOUR_DESCRIPTION_REQUIRED)
    @Schema(description = "Mô tả chi tiết về tour", example = "Chinh phục đỉnh Fansipan - Nóc nhà Đông Dương")
    private String description;

    @NotNull(message = MessageConstant.TOUR_DIFFICULTY_REQUIRED)
    @Schema(description = "Độ khó của tour (EASY, MODERATE, HARD, EXPERT)", example = "HARD")
    private DifficultyLevel difficulty;

    @NotBlank(message = MessageConstant.TOUR_LOCATION_REQUIRED)
    @Schema(description = "Địa điểm tổ chức tour", example = "Sapa, Lào Cai")
    private String location;

    @NotNull(message = MessageConstant.TOUR_DURATION_REQUIRED)
    @Min(value = 1, message = MessageConstant.TOUR_DURATION_MIN)
    @Schema(description = "Số ngày diễn ra tour", example = "2")
    private Integer durationDays;

    @NotNull(message = MessageConstant.TOUR_BASE_PRICE_REQUIRED)
    @DecimalMin(value = "0.0", inclusive = false, message = MessageConstant.TOUR_BASE_PRICE_MIN)
    @Schema(description = "Giá cơ bản của tour", example = "1500000")
    private BigDecimal basePrice;

    @NotNull(message = MessageConstant.TOUR_MIN_CAPACITY_REQUIRED)
    @Min(value = 1, message = MessageConstant.TOUR_MIN_CAPACITY_MIN)
    @Schema(description = "Số lượng người tối thiểu để tổ chức tour", example = "5")
    private Integer minCapacity = 1;

    @NotNull(message = MessageConstant.TOUR_MAX_CAPACITY_REQUIRED)
    @Min(value = 1, message = MessageConstant.TOUR_MAX_CAPACITY_MIN)
    @Schema(description = "Số lượng người tối đa của tour", example = "15")
    private Integer maxCapacity;

    @Schema(description = "Tổng quãng đường di chuyển (km)", example = "12.5")
    private BigDecimal totalDistanceKm;

    @Schema(description = "Những điểm nổi bật của tour", example = "- Ngắm biển mây\n- Chinh phục đỉnh núi cao nhất Đông Dương")
    private String highlights;

    @Schema(description = "Các dịch vụ bao gồm trong giá", example = "- HDV địa phương\n- Ăn uống các bữa")
    private String includes;

    @Schema(description = "Các dịch vụ không bao gồm", example = "- Chi phí cá nhân\n- Vé cáp treo")
    private String excludes;

    @Schema(description = "URL ảnh bìa của tour", example = "https://example.com/images/fansipan.jpg")
    private String coverImageUrl;
}
