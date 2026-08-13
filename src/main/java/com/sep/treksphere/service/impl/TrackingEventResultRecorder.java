package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.response.TrackingEventResult;
import com.sep.treksphere.entity.TrackingIngestedEvent;
import com.sep.treksphere.enums.tracking.TrackingEventStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.repository.TrackingIngestedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingEventResultRecorder {

    private final TrackingIngestedEventRepository eventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TrackingEventResult recordFailure(UUID eventId, RuntimeException exception) {
        TrackingIngestedEvent event = eventRepository.findById(eventId).orElseThrow();
        boolean conflict = exception instanceof AppException appException
                && appException.getErrorCode().getHttpStatus() == HttpStatus.CONFLICT;
        event.setProcessingStatus(conflict ? TrackingEventStatus.CONFLICT : TrackingEventStatus.REJECTED);
        event.setProcessedAt(Instant.now());
        if (exception instanceof AppException appException) {
            event.setErrorCode(appException.getErrorCode().name());
        } else {
            event.setErrorCode("INVALID_EVENT_PAYLOAD");
        }
        event.setResultMessage(limit(exception.getMessage()));
        eventRepository.save(event);
        return toResult(event, event.getProcessingStatus().name());
    }

    public TrackingEventResult toResult(TrackingIngestedEvent event, String status) {
        return TrackingEventResult.builder()
                .clientEventId(event.getClientEventId())
                .sequenceNumber(event.getSequenceNumber())
                .status(status)
                .code(event.getErrorCode())
                .message(event.getResultMessage())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .resultRevision(event.getResultRevision())
                .build();
    }

    private String limit(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
