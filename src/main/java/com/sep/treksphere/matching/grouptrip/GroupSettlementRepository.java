package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.matching.grouptrip.GroupSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupSettlementRepository extends JpaRepository<GroupSettlement, UUID> {
}
