package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.CoordinatorAllocationDto;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.TourSession;
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
}
