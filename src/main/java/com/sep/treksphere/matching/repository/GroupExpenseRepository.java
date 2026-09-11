package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    Optional<GroupExpense> findByGroupExpenseIdAndGroupTrip_MatchingGroup_MatchingGroupId(
            UUID groupExpenseId, UUID matchingGroupId);

    Page<GroupExpense> findByGroupTrip_MatchingGroup_MatchingGroupId(
            UUID matchingGroupId, Pageable pageable);

    @Query("SELECT e FROM GroupExpense e " +
           "WHERE e.groupTrip.matchingGroup.matchingGroupId = :groupId " +
           "AND (CAST(:keyword AS string) IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(e.note) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "AND (:paidByMemberId IS NULL OR e.paidBy.matchingMemberId = :paidByMemberId) " +
           "AND (:scope IS NULL OR e.beneficiaryScope = :scope)")
    Page<GroupExpense> findByGroupIdWithFilter(
            @Param("groupId") UUID groupId,
            @Param("keyword") String keyword,
            @Param("paidByMemberId") UUID paidByMemberId,
            @Param("scope") BeneficiaryScope scope,
            Pageable pageable);

    List<GroupExpense> findByGroupTrip_MatchingGroup_MatchingGroupId(
            UUID matchingGroupId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM GroupExpense e " +
           "WHERE e.groupTrip.matchingGroup.matchingGroupId = :groupId")
    BigDecimal sumAmountByMatchingGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT COUNT(e) FROM GroupExpense e " +
           "WHERE e.groupTrip.matchingGroup.matchingGroupId = :groupId")
    long countByMatchingGroupId(@Param("groupId") UUID groupId);
}
