package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorMapper {

    @Mapping(target = "vendorID", expression = "java(vendor.getVendorID() != null ? vendor.getVendorID().toString() : null)")
    @Mapping(target = "managerUser", source = "managerUser")
    VendorResponse toVendorResponse(Vendor vendor);
}
