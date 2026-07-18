package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VendorProfileUpdateRequest {
    
    private String description;

    @Email(message = MessageConstant.EMAIL_INVALID)
    private String contactEmail;

    @Pattern(regexp = ValidationConstant.VENDOR_PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String contactPhone;

    private String bankAccount;
    
    private String bankName;
    
    private MultipartFile logo;
    
    private MultipartFile paymentQr;
}
