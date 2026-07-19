package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.entity.VendorApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorApplicationMapper {

    @Mapping(target = "vendorApplicationId", ignore = true)
    @Mapping(target = "applicant", ignore = true)
    @Mapping(target = "applicationStatus", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "businessLicenseUrl", ignore = true)
    VendorApplication toEntity(VendorApplicationRequest request);

    @Mapping(target = "applicant", source = "applicant")
    VendorApplicationResponse toResponse(VendorApplication entity);
}
