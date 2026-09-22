package com.sep.treksphere.vendor;

import com.sep.treksphere.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorMapper {

    @Mapping(target = "vendorId", expression = "java(vendor.getVendorId() != null ? vendor.getVendorId().toString() : null)")
    @Mapping(target = "manager", source = "manager")
    VendorResponse toVendorResponse(Vendor vendor);

    VendorProfileResponse toVendorProfileResponse(Vendor vendor);
}
