package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.logistics.CoordinatorScheduleResponse;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CoordinatorScheduleMapper {

    CoordinatorScheduleResponse toDto(CoordinatorSchedule coordinatorSchedule);

    default String map(Role role) {
        return role != null ? role.getRoleName() : null;
    }
}
