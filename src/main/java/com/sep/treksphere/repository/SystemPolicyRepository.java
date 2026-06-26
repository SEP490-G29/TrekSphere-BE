package com.sep.treksphere.repository;

import com.sep.treksphere.entity.SystemPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SystemPolicyRepository extends JpaRepository<SystemPolicy, UUID> {
}
