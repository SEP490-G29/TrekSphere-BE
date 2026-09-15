-- Support completed-trip personalization lookups by owner and accepted member.
CREATE INDEX ix_matching_group_owner_completed_tour
    ON matching_group (owner_id, tour_id)
    WHERE is_deleted = FALSE
      AND status = 'COMPLETED'
      AND tour_id IS NOT NULL;

CREATE INDEX ix_matching_member_user_accepted_group
    ON matching_member (user_id, matching_group_id)
    WHERE is_deleted = FALSE
      AND status = 'ACCEPTED';
