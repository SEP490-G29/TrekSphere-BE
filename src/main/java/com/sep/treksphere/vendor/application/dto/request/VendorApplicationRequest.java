package com.sep.treksphere.vendor.application.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.constant.ValidationConstant;
import jakarta.validation.constraints.Email;
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

    private String companyName;

    @Email(message = MessageConstant.EMAIL_INVALID)
    private String contactEmail;

    @Pattern(regexp = "^$|" + ValidationConstant.VENDOR_PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String contactPhone;

    private String businessDescription;

    @Pattern(regexp = "^$|" + ValidationConstant.TAX_CODE_REGEX, message = MessageConstant.TAX_CODE_INVALID)
    private String taxCode;

    private MultipartFile businessLicense;

    private String businessAddress;
    private String legalRepresentativeName;
    private String legalRepresentativePosition;
    private String websiteUrl;
}
