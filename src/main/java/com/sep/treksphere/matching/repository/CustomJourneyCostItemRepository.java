package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.CustomJourneyCostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJourneyCostItemRepository extends JpaRepository<CustomJourneyCostItem, UUID> {

    List<CustomJourneyCostItem> findByCustomJourney_CustomJourneyIdAndIsDeletedFalse(UUID customJourneyId);

    Optional<CustomJourneyCostItem> findByCustomJourneyCostItemIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
            UUID customJourneyCostItemId, UUID customJourneyId);
}
