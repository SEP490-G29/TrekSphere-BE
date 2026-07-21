package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.request.UpdateVendorEquipmentRequest;
import com.sep.treksphere.dto.request.VendorEquipmentRequest;
import com.sep.treksphere.dto.response.VendorEquipmentDto;
import com.sep.treksphere.entity.VendorEquipment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VendorEquipmentMapper {

    @Mapping(source = "vendor.vendorId", target = "vendorId")
    VendorEquipmentDto toDto(VendorEquipment entity);

    @Mapping(target = "equipmentId", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    VendorEquipment toEntity(VendorEquipmentRequest request);

    @Mapping(target = "equipmentId", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateVendorEquipmentRequest request, @MappingTarget VendorEquipment entity);
}
