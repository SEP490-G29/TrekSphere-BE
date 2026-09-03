package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.matching.grouptrip.CustomJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomJourneyRepository extends JpaRepository<CustomJourney, UUID> {
}
