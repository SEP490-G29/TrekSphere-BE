package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.CustomJourneyActivity;
import com.sep.treksphere.matching.enums.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJourneyActivityRepository extends JpaRepository<CustomJourneyActivity, UUID> {

    List<CustomJourneyActivity> findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscTimeSlotAscActivityOrderAsc(
            UUID customJourneyId);

    List<CustomJourneyActivity> findByCustomJourney_CustomJourneyIdAndDayNoAndIsDeletedFalseOrderByTimeSlotAscActivityOrderAsc(
            UUID customJourneyId, Integer dayNo);

    Optional<CustomJourneyActivity> findByCustomJourneyActivityIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
            UUID activityId, UUID customJourneyId);

    boolean existsByCustomJourney_CustomJourneyIdAndDayNoAndTimeSlotAndActivityOrderAndIsDeletedFalse(
            UUID customJourneyId, Integer dayNo, TimeSlot timeSlot, Integer activityOrder);

    boolean existsByCustomJourney_CustomJourneyIdAndDayNoAndTimeSlotAndActivityOrderAndCustomJourneyActivityIdNotAndIsDeletedFalse(
            UUID customJourneyId, Integer dayNo, TimeSlot timeSlot, Integer activityOrder, UUID activityId);
}
