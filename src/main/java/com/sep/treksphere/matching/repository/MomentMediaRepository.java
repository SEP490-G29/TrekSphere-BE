package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.MomentMedia;
import com.sep.treksphere.matching.enums.MomentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MomentMediaRepository extends JpaRepository<MomentMedia, UUID> {

    List<MomentMedia> findByMoment_MomentIdOrderBySortOrderAsc(UUID momentId);

    Page<MomentMedia> findByMoment_MatchingGroup_MatchingGroupIdAndMoment_StatusAndMoment_IsDeletedFalseOrderByCreatedAtDesc(
            UUID matchingGroupId, MomentStatus status, Pageable pageable);

    Page<MomentMedia> findByMoment_MatchingGroup_MatchingGroupIdAndMoment_IsDeletedFalseOrderByCreatedAtDesc(
            UUID matchingGroupId, Pageable pageable);

    void deleteByMoment_MomentId(UUID momentId);
}
