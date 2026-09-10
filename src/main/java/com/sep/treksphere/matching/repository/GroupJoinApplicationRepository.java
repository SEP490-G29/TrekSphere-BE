package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupJoinApplication;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.enums.JoinApplicationStatus;
import com.sep.treksphere.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupJoinApplicationRepository extends JpaRepository<GroupJoinApplication, UUID> {

    Optional<GroupJoinApplication> findByMatchingGroupAndApplicantAndStatusAndIsDeletedFalse(
            MatchingGroup matchingGroup,
            User applicant,
            JoinApplicationStatus status
    );

    @Query("""
        SELECT gja FROM GroupJoinApplication gja
        JOIN FETCH gja.matchingGroup mg
        JOIN FETCH gja.applicant a
        WHERE gja.applicationId = :applicationId
          AND gja.isDeleted = false
    """)
    Optional<GroupJoinApplication> findDetailById(@Param("applicationId") UUID applicationId);

    @Query("""
        SELECT gja FROM GroupJoinApplication gja
        JOIN FETCH gja.applicant a
        WHERE gja.matchingGroup.matchingGroupId = :groupId
          AND gja.status = :status
          AND gja.isDeleted = false
        ORDER BY gja.createdAt DESC
    """)
    Page<GroupJoinApplication> findByGroupIdAndStatus(
            @Param("groupId") UUID groupId,
            @Param("status") JoinApplicationStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT gja FROM GroupJoinApplication gja
        JOIN FETCH gja.applicant a
        WHERE gja.matchingGroup.matchingGroupId = :groupId
          AND gja.isDeleted = false
        ORDER BY gja.createdAt DESC
    """)
    Page<GroupJoinApplication> findByGroupId(
            @Param("groupId") UUID groupId,
            Pageable pageable
    );

    @Query(
        value = """
            SELECT gja FROM GroupJoinApplication gja
            JOIN FETCH gja.matchingGroup mg
            LEFT JOIN FETCH mg.tour t
            LEFT JOIN FETCH mg.customJourney cj
            JOIN FETCH mg.owner o
            WHERE gja.applicant.userId = :userId
              AND (CAST(:status AS string) IS NULL OR gja.status = :status)
              AND gja.isDeleted = false
              AND mg.isDeleted = false
            ORDER BY gja.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(gja) FROM GroupJoinApplication gja
            JOIN gja.matchingGroup mg
            WHERE gja.applicant.userId = :userId
              AND (CAST(:status AS string) IS NULL OR gja.status = :status)
              AND gja.isDeleted = false
              AND mg.isDeleted = false
        """
    )
    Page<GroupJoinApplication> findMyApplications(
            @Param("userId") UUID userId,
            @Param("status") JoinApplicationStatus status,
            Pageable pageable
    );

    boolean existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
            UUID groupId,
            UUID userId,
            JoinApplicationStatus status
    );

    List<GroupJoinApplication> findByMatchingGroup_MatchingGroupIdAndApplicant_UserIdOrderByCreatedAtDesc(
            UUID groupId,
            UUID userId
    );
}
