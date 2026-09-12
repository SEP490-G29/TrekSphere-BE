package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MomentRepository extends JpaRepository<Moment, UUID> {

    Optional<Moment> findByMomentIdAndIsDeletedFalse(UUID momentId);

    Page<Moment> findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(
            UUID matchingGroupId, MomentStatus status, Pageable pageable);

    Page<Moment> findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(
            UUID matchingGroupId, Pageable pageable);

    List<Moment> findByMatchingGroup_MatchingGroupIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndStatusAndIsDeletedFalse(
            UUID matchingGroupId, MomentStatus status);

    List<Moment> findByMatchingGroup_MatchingGroupIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndIsDeletedFalse(
            UUID matchingGroupId);

    Page<Moment> findByAuthorUser_UserIdAndIsDeletedFalse(
            UUID userId, Pageable pageable);

    List<Moment> findByAuthorUser_UserIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndIsDeletedFalse(
            UUID userId);

    Page<Moment> findByAuthorUser_UserIdAndVisibilityAndStatusAndIsDeletedFalse(
            UUID userId, MomentVisibility visibility, MomentStatus status, Pageable pageable);
}
