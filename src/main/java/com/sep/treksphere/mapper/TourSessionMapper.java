package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.StaffScheduleResponse;
import com.sep.treksphere.dto.response.CoordinatorAllocationDto;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.PorterSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.dto.response.PorterAllocationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TourSessionMapper {

    @Mapping(source = "tourSessionId", target = "sessionId")
    @Mapping(source = "tourSchedule.tour.tourId", target = "tourId")
    @Mapping(source = "tourSchedule.tour.tourName", target = "tourName")
    @Mapping(source = "tourSchedule.departureDate", target = "departureDate")
    @Mapping(source = "tourSchedule.returnDate", target = "returnDate")
    TourSessionSummaryResponse toSummaryResponse(TourSession tourSession);

    @Mapping(source = "tourSessionId", target = "sessionId")
    @Mapping(source = "tourSchedule.tour.tourId", target = "tourId")
    @Mapping(source = "tourSchedule.tour.tourName", target = "tourName")
    @Mapping(source = "tourSchedule.departureDate", target = "departureDate")
    @Mapping(source = "tourSchedule.returnDate", target = "returnDate")
    TourSessionAllocationResponse toAllocationResponse(TourSession tourSession);

    @Mapping(source = "coordinator.userId", target = "coordinatorId")
    @Mapping(source = "coordinator.fullName", target = "fullName")
    @Mapping(source = "coordinator.email", target = "email")
    @Mapping(source = "coordinator.phone", target = "phone")
    @Mapping(source = "coordinator.avatarUrl", target = "avatar")
    CoordinatorAllocationDto toCoordinatorAllocationDto(CoordinatorSchedule coordinatorSchedule);

    List<CoordinatorAllocationDto> toCoordinatorAllocationDtoList(List<CoordinatorSchedule> coordinatorSchedules);

    @Mapping(source = "porter.porterId", target = "porterId")
    @Mapping(source = "porter.fullName", target = "fullName")
    @Mapping(source = "porter.phone", target = "phone")
    PorterAllocationDto toPorterAllocationDto(PorterSchedule porterSchedule);

    List<PorterAllocationDto> toPorterAllocationDtoList(List<PorterSchedule> porterSchedules);

    @Mapping(source = "coordinator.userId", target = "coordinatorId")
    @Mapping(source = "coordinator.fullName", target = "coordinatorName")
    @Mapping(source = "tourSession.tourSessionId", target = "tourSessionId")
    @Mapping(source = "tourSession.tourSchedule.tour.tourName", target = "tourName")
    @Mapping(source = "tourSession.tourSchedule.departureDate", target = "departureDate")
    @Mapping(source = "tourSession.tourSchedule.returnDate", target = "returnDate")
    @Mapping(source = "tourSession.status", target = "status")
    StaffScheduleResponse toStaffScheduleResponse(CoordinatorSchedule coordinatorSchedule);

    List<StaffScheduleResponse> toStaffScheduleResponseList(List<CoordinatorSchedule> coordinatorSchedules);
}
