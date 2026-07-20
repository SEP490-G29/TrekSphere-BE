package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.request.PorterProfileRequest;
import com.sep.treksphere.dto.request.UpdatePorterProfileRequest;
import com.sep.treksphere.dto.response.PorterProfileDto;
import com.sep.treksphere.entity.PorterProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PorterProfileMapper {

    PorterProfileDto toDto(PorterProfile entity);

    @Mapping(target = "porterId", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "joinedDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    PorterProfile toEntity(PorterProfileRequest request);

    @Mapping(target = "porterId", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "joinedDate", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget PorterProfile entity, UpdatePorterProfileRequest request);
}
