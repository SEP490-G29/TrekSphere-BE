package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorMapper {

    @Mapping(target = "vendorId", expression = "java(vendor.getVendorId() != null ? vendor.getVendorId().toString() : null)")
    @Mapping(target = "manager", source = "manager")
    VendorResponse toVendorResponse(Vendor vendor);
}
