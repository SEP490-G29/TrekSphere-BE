package com.sep.treksphere.repository;

import com.sep.treksphere.entity.BookingPolicySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingPolicySnapshotRepository extends JpaRepository<BookingPolicySnapshot, UUID> {
}
