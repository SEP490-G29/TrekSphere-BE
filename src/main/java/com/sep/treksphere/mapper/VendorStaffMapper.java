package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.VendorStaffResponse;
import com.sep.treksphere.entity.VendorStaff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface VendorStaffMapper {

    @Mapping(target = "vendorStaffId", expression = "java(vendorStaff.getVendorStaffId() != null ? vendorStaff.getVendorStaffId().toString() : null)")
    @Mapping(target = "vendorId", expression = "java(vendorStaff.getVendor() != null && vendorStaff.getVendor().getVendorId() != null ? vendorStaff.getVendor().getVendorId().toString() : null)")
    @Mapping(target = "user", source = "user")
    VendorStaffResponse toVendorStaffResponse(VendorStaff vendorStaff);
}
