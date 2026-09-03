package com.sep.treksphere.report;

import com.sep.treksphere.report.ReportContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.sep.treksphere.report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReportContentRepository extends JpaRepository<ReportContent, UUID> {
    
    @Query("SELECT r FROM ReportContent r WHERE (CAST(:status AS string) IS NULL OR r.status = :status) AND r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<ReportContent> findByStatusAndIsDeletedFalse(@Param("status") ReportStatus status, Pageable pageable);
}
