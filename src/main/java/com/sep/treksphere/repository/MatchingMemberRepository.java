package com.sep.treksphere.repository;

import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import com.sep.treksphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingMemberRepository extends JpaRepository<MatchingMember, UUID> {

    Optional<MatchingMember> findByMatchingGroupAndUser(MatchingGroup matchingGroup, User user);
}
