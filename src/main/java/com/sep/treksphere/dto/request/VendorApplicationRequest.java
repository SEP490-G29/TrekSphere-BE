package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorApplicationRequest {

    @NotBlank(message = MessageConstant.COMPANY_NAME_REQUIRED)
    private String companyName;

    @NotBlank(message = MessageConstant.CONTACT_EMAIL_REQUIRED)
    @Email(message = MessageConstant.EMAIL_INVALID)
    private String contactEmail;

    @NotBlank(message = MessageConstant.CONTACT_PHONE_REQUIRED)
    @Pattern(regexp = ValidationConstant.VENDOR_PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String contactPhone;

    private String businessDescription;

    @NotBlank(message = MessageConstant.TAX_CODE_REQUIRED)
    @Pattern(regexp = ValidationConstant.TAX_CODE_REGEX, message = MessageConstant.TAX_CODE_INVALID)
    private String taxCode;

    @NotNull(message = MessageConstant.BUSINESS_LICENSE_REQUIRED)
    private MultipartFile businessLicense;
}
