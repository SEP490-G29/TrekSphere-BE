package com.sep.treksphere.vendor.mapper;

import com.sep.treksphere.user.mapper.UserMapper;
import com.sep.treksphere.vendor.dto.response.VendorProfileResponse;
import com.sep.treksphere.vendor.dto.response.VendorResponse;
import com.sep.treksphere.vendor.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorMapper {

    @Mapping(target = "vendorId", expression = "java(vendor.getVendorId() != null ? vendor.getVendorId().toString() : null)")
    @Mapping(target = "manager", source = "manager")
    VendorResponse toVendorResponse(Vendor vendor);

    VendorProfileResponse toVendorProfileResponse(Vendor vendor);
}
