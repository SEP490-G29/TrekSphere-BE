package com.sep.treksphere.user.repository;

import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.isDeleted = false")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);
    
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
    
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, UUID userId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phone IN :phones AND u.userId <> :userId AND u.isDeleted = false")
    boolean existsByPhoneInAndUserIdNot(@Param("phones") Collection<String> phones, @Param("userId") UUID userId);

    List<User> findAllByUserIdInAndStatusAndIsDeletedFalse(
            Collection<UUID> userIds,
            UserStatus status
    );

    List<User> findDistinctByRoles_RoleNameAndIsDeletedFalse(String roleName);

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
