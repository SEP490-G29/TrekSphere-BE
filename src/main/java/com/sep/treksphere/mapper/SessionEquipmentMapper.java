package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.request.AssignEquipmentRequest;
import com.sep.treksphere.entity.SessionEquipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionEquipmentMapper {
    @Mapping(target = "tourSession", ignore = true)
    @Mapping(target = "equipment", ignore = true)
    SessionEquipment toEntity(AssignEquipmentRequest request);
}
