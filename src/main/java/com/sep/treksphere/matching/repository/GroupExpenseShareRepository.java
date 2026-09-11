package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GroupExpenseShareRepository extends JpaRepository<GroupExpenseShare, UUID> {

    List<GroupExpenseShare> findByGroupExpense_GroupExpenseId(UUID groupExpenseId);

    @Modifying
    @Query("UPDATE GroupExpenseShare s SET s.isDeleted = true, s.deletedAt = :now WHERE s.groupExpense.groupExpenseId = :groupExpenseId")
    void softDeleteByGroupExpenseId(@Param("groupExpenseId") UUID groupExpenseId, @Param("now") LocalDateTime now);

    List<GroupExpenseShare> findByMatchingMember_MatchingMemberId(UUID matchingMemberId);
}
