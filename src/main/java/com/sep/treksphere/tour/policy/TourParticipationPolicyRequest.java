package com.sep.treksphere.tour.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourParticipationPolicyRequest {

    @Schema(description = "Tuổi tối thiểu", example = "18")
    private Integer minAge;

    @Schema(description = "Tuổi tối đa", example = "60")
    private Integer maxAge;

    @Schema(description = "Chiều cao tối thiểu (cm)", example = "150.0")
    private BigDecimal minHeightCm;

    @Schema(description = "Chiều cao tối đa (cm)", example = "195.0")
    private BigDecimal maxHeightCm;

    @Schema(description = "Cân nặng tối thiểu (kg)", example = "45.0")
    private BigDecimal minWeightKg;

    @Schema(description = "Cân nặng tối đa (kg)", example = "100.0")
    private BigDecimal maxWeightKg;

    @Schema(description = "Mức độ thể lực yêu cầu (ANY, BASIC, MODERATE, HIGH, EXTREME)", example = "MODERATE")
    private String fitnessLevel;

    @Schema(description = "Yêu cầu sức khỏe", example = "Chạy bộ 3km/ngày liên tục 2 tuần trước chuyến đi")
    private String healthRequirements;

    @Schema(description = "Tình trạng bệnh lý hạn chế", example = "Không có tiền sử bệnh tim mạch, hen suyễn")
    private String restrictedMedicalConditions;

    @Schema(description = "Kinh nghiệm yêu cầu", example = "Đã từng tham gia ít nhất 1 tour trekking")
    private String requiredExperience;

    @Schema(description = "Kỹ năng yêu cầu", example = "Kỹ năng leo dốc, sử dụng gậy leo núi")
    private String requiredSkills;

    @Schema(description = "Trang bị bắt buộc", example = "Giày trekking, balo 30L, gậy leo núi")
    private String requiredEquipment;

    @Schema(description = "Giấy tờ bắt buộc", example = "CCCD/Hộ chiếu bản gốc")
    private String requiredDocuments;

    @Schema(description = "Yêu cầu khai báo y tế", example = "true")
    private Boolean requiresHealthDeclaration;

    @Schema(description = "Yêu cầu giấy khám sức khỏe", example = "false")
    private Boolean requiresMedicalCertificate;

    @Schema(description = "Độ tuổi cần người giám hộ đi kèm", example = "18")
    private Integer guardianRequiredUnderAge;

    @Schema(description = "Yêu cầu bổ sung khác", example = "Không sử dụng chất kích thích trong suốt chuyến đi")
    private String additionalRequirements;
}
