package com.sep.treksphere.tour.recommendation;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourBehaviorEventServiceTest {

    private TourBehaviorEventRepository eventRepository;
    private TourRepository tourRepository;
    private UserRepository userRepository;
    private TourBehaviorEventService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID tourId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventRepository = mock(TourBehaviorEventRepository.class);
        tourRepository = mock(TourRepository.class);
        userRepository = mock(UserRepository.class);
        service = new TourBehaviorEventService(eventRepository, tourRepository, userRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordsDistinctEventsAndDeduplicatesRepeatedBehaviorInOneBatch() {
        User user = new User();
        user.setUserId(userId);
        user.setStatus(UserStatus.ACTIVE);
        Tour tour = new Tour();
        tour.setTourId(tourId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tourRepository.findPublishedByIds(argThat(
                ids -> ids.size() == 1 && ids.contains(tourId))))
                .thenReturn(List.of(tour));
        when(eventRepository.findRecentEvents(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(List.of(tourId)),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        TourBehaviorEventBatchRequest request = TourBehaviorEventBatchRequest.builder()
                .events(List.of(
                        event(TourBehaviorEventType.VIEW, " session-1 "),
                        event(TourBehaviorEventType.VIEW, "session-1"),
                        event(TourBehaviorEventType.CLICK, "session-1")))
                .build();

        TourBehaviorEventBatchResponse response = service.recordEvents(userId, request);

        assertEquals(2, response.getRecordedCount());
        assertEquals(1, response.getDeduplicatedCount());
        ArgumentCaptor<List<TourBehaviorEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals("session-1", captor.getValue().getFirst().getSessionId());
    }

    @Test
    void rejectsBehaviorForAnUnavailableTour() {
        User user = new User();
        user.setUserId(userId);
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tourRepository.findPublishedByIds(argThat(
                ids -> ids.size() == 1 && ids.contains(tourId))))
                .thenReturn(List.of());

        TourBehaviorEventBatchRequest request = TourBehaviorEventBatchRequest.builder()
                .events(List.of(event(TourBehaviorEventType.VIEW, null)))
                .build();

        assertThrows(AppException.class, () -> service.recordEvents(userId, request));
    }

    @Test
    void clearsOnlyTheCurrentTrekkersBehaviorHistory() {
        when(eventRepository.deleteByUserId(userId)).thenReturn(7);

        assertEquals(7, service.clearHistory(userId));
        verify(eventRepository).deleteByUserId(userId);
    }

    private TourBehaviorEventItemRequest event(
            TourBehaviorEventType type, String sessionId) {
        return TourBehaviorEventItemRequest.builder()
                .tourId(tourId)
                .eventType(type)
                .source(TourBehaviorSource.RECOMMENDATION)
                .sessionId(sessionId)
                .displayPosition(0)
                .build();
    }
}
