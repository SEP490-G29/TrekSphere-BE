package com.sep.treksphere.vendor.application;

import com.sep.treksphere.user.UserMapper;
import com.sep.treksphere.vendor.application.dto.request.VendorApplicationRequest;
import com.sep.treksphere.vendor.application.dto.response.VendorApplicationResponse;
import com.sep.treksphere.vendor.application.VendorApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorApplicationMapper {

    @Mapping(target = "vendorApplicationId", ignore = true)
    @Mapping(target = "applicant", ignore = true)
    @Mapping(target = "applicationStatus", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "businessLicenseUrl", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    VendorApplication toEntity(VendorApplicationRequest request);

    @Mapping(target = "applicant", source = "applicant")
    @Mapping(target = "reviewedBy", source = "reviewedBy.userId")
    @Mapping(target = "vendorId", source = "vendor.vendorId")
    VendorApplicationResponse toResponse(VendorApplication entity);
}
