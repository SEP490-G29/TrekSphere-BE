package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.matching.grouptrip.GroupExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupExpenseShareRepository extends JpaRepository<GroupExpenseShare, UUID> {
}
