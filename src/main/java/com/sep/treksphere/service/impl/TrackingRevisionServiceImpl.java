package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.TrackingSessionRevision;
import com.sep.treksphere.repository.TrackingSessionRevisionRepository;
import com.sep.treksphere.service.TrackingRevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingRevisionServiceImpl implements TrackingRevisionService {

    private final TrackingSessionRevisionRepository revisionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public long increment(UUID sessionId) {
        jdbcTemplate.update("""
                INSERT INTO tracking_session_revision(tour_session_id, revision, updated_at)
                VALUES (?, 0, CURRENT_TIMESTAMP)
                ON CONFLICT (tour_session_id) DO NOTHING
                """, sessionId);
        TrackingSessionRevision revision = revisionRepository.findByIdForUpdate(sessionId).orElseThrow();
        revision.setRevision(revision.getRevision() + 1);
        revisionRepository.save(revision);
        return revision.getRevision();
    }
}
