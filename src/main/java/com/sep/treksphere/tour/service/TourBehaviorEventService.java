package com.sep.treksphere.tour.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.tour.dto.request.TourBehaviorEventBatchRequest;
import com.sep.treksphere.tour.dto.request.TourBehaviorEventItemRequest;
import com.sep.treksphere.tour.dto.response.TourBehaviorEventBatchResponse;
import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.entity.TourBehaviorEvent;
import com.sep.treksphere.tour.enums.TourBehaviorEventType;
import com.sep.treksphere.tour.repository.TourBehaviorEventRepository;
import com.sep.treksphere.tour.repository.TourRepository;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.enums.UserStatus;
import com.sep.treksphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourBehaviorEventService {

    private static final int RETENTION_DAYS = 180;
    private static final Duration MAX_DEDUPLICATION_WINDOW = Duration.ofHours(24);

    private final TourBehaviorEventRepository eventRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;

    @Transactional
    public TourBehaviorEventBatchResponse recordEvents(
            UUID userId, TourBehaviorEventBatchRequest request) {
        User user = userRepository.findById(userId)
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Set<UUID> tourIds = request.getEvents().stream()
                .map(TourBehaviorEventItemRequest::getTourId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Tour> tours = tourRepository.findPublishedByIds(tourIds).stream()
                .collect(Collectors.toMap(Tour::getTourId, Function.identity()));
        if (tours.size() != tourIds.size()) {
            throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        List<TourBehaviorEvent> recentEvents = eventRepository.findRecentEvents(
                userId, tourIds, now.minus(MAX_DEDUPLICATION_WINDOW));
        Map<EventKey, LocalDateTime> latestByKey = latestEventsByKey(recentEvents);
        List<TourBehaviorEvent> eventsToSave = new ArrayList<>();
        int deduplicatedCount = 0;

        for (TourBehaviorEventItemRequest item : request.getEvents()) {
            EventKey key = new EventKey(item.getTourId(), item.getEventType());
            LocalDateTime latest = latestByKey.get(key);
            if (latest != null && latest.isAfter(now.minus(deduplicationWindow(item.getEventType())))) {
                deduplicatedCount++;
                continue;
            }

            TourBehaviorEvent event = new TourBehaviorEvent();
            event.setUser(user);
            event.setTour(tours.get(item.getTourId()));
            event.setEventType(item.getEventType());
            event.setSource(item.getSource());
            event.setSessionId(normalizeSessionId(item.getSessionId()));
            event.setDisplayPosition(item.getDisplayPosition());
            event.setOccurredAt(now);
            eventsToSave.add(event);
            latestByKey.put(key, now);
        }

        List<TourBehaviorEvent> saved = eventRepository.saveAll(eventsToSave);
        return TourBehaviorEventBatchResponse.builder()
                .recordedCount(saved.size())
                .deduplicatedCount(deduplicatedCount)
                .eventIds(saved.stream().map(TourBehaviorEvent::getBehaviorEventId).toList())
                .build();
    }

    @Transactional
    public int clearHistory(UUID userId) {
        return eventRepository.deleteByUserId(userId);
    }

    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void deleteExpiredEvents() {
        eventRepository.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
    }

    private Map<EventKey, LocalDateTime> latestEventsByKey(
            Collection<TourBehaviorEvent> events) {
        Map<EventKey, LocalDateTime> latest = new HashMap<>();
        for (TourBehaviorEvent event : events) {
            EventKey key = new EventKey(event.getTour().getTourId(), event.getEventType());
            latest.merge(key, event.getOccurredAt(),
                    (left, right) -> left.isAfter(right) ? left : right);
        }
        return latest;
    }

    private Duration deduplicationWindow(TourBehaviorEventType eventType) {
        return switch (eventType) {
            case IMPRESSION -> Duration.ofMinutes(30);
            case VIEW -> Duration.ofMinutes(10);
            case CLICK -> Duration.ofMinutes(2);
            case SAVE, UNSAVE -> Duration.ofHours(1);
            case DISMISS -> Duration.ofHours(24);
        };
    }

    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? null : sessionId.strip();
    }

    private record EventKey(UUID tourId, TourBehaviorEventType eventType) {
    }
}
