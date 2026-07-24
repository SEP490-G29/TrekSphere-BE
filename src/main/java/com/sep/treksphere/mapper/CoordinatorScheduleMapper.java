package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.CoordinatorScheduleResponse;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface CoordinatorScheduleMapper {

    @Mapping(target = "tourSessionId", expression = "java(schedule.getTourSession() != null ? schedule.getTourSession().getTourSessionId() : null)")
    @Mapping(target = "sessionStatus", expression = "java(schedule.getTourSession() != null ? schedule.getTourSession().getStatus() : null)")
    @Mapping(target = "tourId", expression = "java(schedule.getTourSession() != null && schedule.getTourSession().getTourSchedule() != null && schedule.getTourSession().getTourSchedule().getTour() != null ? schedule.getTourSession().getTourSchedule().getTour().getTourId() : null)")
    @Mapping(target = "tourName", expression = "java(schedule.getTourSession() != null && schedule.getTourSession().getTourSchedule() != null && schedule.getTourSession().getTourSchedule().getTour() != null ? schedule.getTourSession().getTourSchedule().getTour().getTourName() : null)")
    @Mapping(target = "departureDate", expression = "java(schedule.getTourSession() != null && schedule.getTourSession().getTourSchedule() != null ? schedule.getTourSession().getTourSchedule().getDepartureDate() : null)")
    @Mapping(target = "returnDate", expression = "java(schedule.getTourSession() != null && schedule.getTourSession().getTourSchedule() != null ? schedule.getTourSession().getTourSchedule().getReturnDate() : null)")
    CoordinatorScheduleResponse toResponse(CoordinatorSchedule schedule);

    com.sep.treksphere.dto.response.logistics.CoordinatorScheduleResponse toDto(CoordinatorSchedule coordinatorSchedule);

    default String map(Role role) {
        return role != null ? role.getRoleName() : null;
    }
}
