package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    
    Optional<Vendor> findByManagerUser_Email(String email);
    
    @Query("SELECT v FROM Vendor v WHERE v.isDeleted = false AND (:keyword IS NULL OR :keyword = '' OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vendor> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
