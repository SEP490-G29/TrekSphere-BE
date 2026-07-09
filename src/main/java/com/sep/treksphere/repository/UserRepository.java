package com.sep.treksphere.repository;

import com.sep.treksphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import com.sep.treksphere.enums.user.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r " +
           "WHERE u.isDeleted = false " +
           "AND (r.roleName IS NULL OR r.roleName <> 'ADMIN') " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:roleName IS NULL OR :roleName = '' OR r.roleName = :roleName) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findAllUsersWithFilter(@Param("status") UserStatus status, 
                                      @Param("roleName") String roleName, 
                                      @Param("keyword") String keyword, 
                                      Pageable pageable);
}
