package com.sep.treksphere.repository;

import com.sep.treksphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
