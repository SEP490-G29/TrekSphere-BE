package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BookingParticipantRequest {

    @NotBlank(message = MessageConstant.BOOKING_FULL_NAME_REQUIRED)
    @Schema(description = "Họ tên đầy đủ", example = "Nguyễn Văn A")
    private String fullName;

    @NotNull(message = MessageConstant.BOOKING_DOB_REQUIRED)
    @Schema(description = "Ngày sinh của người tham gia", example = "1995-05-15")
    private LocalDate dateOfBirth;

    @NotNull(message = MessageConstant.BOOKING_GENDER_REQUIRED)
    @Schema(description = "Giới tính (MALE, FEMALE, OTHER)", example = "MALE")
    private Gender gender;

    @NotBlank(message = MessageConstant.BOOKING_ID_NUMBER_REQUIRED)
    @Schema(description = "Số căn cước công dân hoặc hộ chiếu", example = "001202001234")
    private String idNumber;

    @NotBlank(message = MessageConstant.BOOKING_PHONE_REQUIRED)
    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    private String phone;

    @Email(message = MessageConstant.BOOKING_EMAIL_INVALID)
    @Schema(description = "Địa chỉ email (không bắt buộc)", example = "nguyenvana@gmail.com")
    private String email;

    @Schema(description = "Địa chỉ cư trú", example = "Cầu Giấy, Hà Nội")
    private String address;

    @Schema(description = "Yêu cầu đặc biệt (nếu có)", example = "Không ăn được thịt bò")
    private String specialRequirements;
}
