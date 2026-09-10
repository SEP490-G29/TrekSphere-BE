-- Align Tour and user preference difficulty with the active Matching Group contract.
ALTER TABLE users DROP CONSTRAINT chk_users_preferred_difficulty;
ALTER TABLE tour DROP CONSTRAINT chk_tour_difficulty;

UPDATE users
SET preferred_difficulty = 'EXTREME'
WHERE preferred_difficulty = 'EXPERT';

UPDATE tour
SET difficulty = 'EXTREME'
WHERE difficulty = 'EXPERT';

ALTER TABLE users
ADD CONSTRAINT chk_users_preferred_difficulty
CHECK (preferred_difficulty IS NULL OR preferred_difficulty IN ('EASY','MODERATE','HARD','EXTREME'));

ALTER TABLE tour
ADD CONSTRAINT chk_tour_difficulty
CHECK (difficulty IN ('EASY','MODERATE','HARD','EXTREME'));
