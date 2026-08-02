package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.LogisticsPassengerResponse;
import com.sep.treksphere.entity.BookingParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogisticsInfoMapper {
    LogisticsPassengerResponse toLogisticsPassengerResponse(BookingParticipant entity);
}
