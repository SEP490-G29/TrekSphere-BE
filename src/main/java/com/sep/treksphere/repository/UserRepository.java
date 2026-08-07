package com.sep.treksphere.repository;

import com.sep.treksphere.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import com.sep.treksphere.enums.user.UserStatus;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.isDeleted = false")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);
    
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    List<User> findAllByUserIdInAndStatusAndIsDeletedFalse(
            Collection<UUID> userIds,
            UserStatus status
    );

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r " +
           "WHERE u.isDeleted = false " +
           "AND (r.roleName IS NULL OR r.roleName <> 'ADMIN') " +
           "AND (CAST(:status AS string) IS NULL OR u.status = :status) " +
           "AND (CAST(:roleName AS string) IS NULL OR CAST(:roleName AS string) = '' OR r.roleName = :roleName) " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<User> findAllUsersWithFilter(@Param("status") UserStatus status, 
                                      @Param("roleName") String roleName, 
                                      @Param("keyword") String keyword, 
                                      Pageable pageable);
}
