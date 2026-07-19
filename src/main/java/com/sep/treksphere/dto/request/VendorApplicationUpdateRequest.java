package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class VendorApplicationUpdateRequest {

    private String companyName;

    @Email(message = MessageConstant.EMAIL_INVALID)
    private String contactEmail;

    @Pattern(regexp = ValidationConstant.VENDOR_PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String contactPhone;

    private String businessDescription;

    @Pattern(regexp = ValidationConstant.TAX_CODE_REGEX, message = MessageConstant.TAX_CODE_INVALID)
    private String taxCode;

    private MultipartFile businessLicense;
}
