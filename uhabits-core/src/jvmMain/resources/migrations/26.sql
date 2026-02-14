-- Add hierarchical structure and direction tracking to Habits table
-- parent_id: nullable FK to Habits(id) for grouping habits under parent routines
-- direction: 0=POSITIVE (things to do), 1=NEGATIVE (things to avoid)

ALTER TABLE habits ADD COLUMN parent_id INTEGER DEFAULT NULL;
ALTER TABLE habits ADD COLUMN direction INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_habits_parent_id ON habits(parent_id);
CREATE INDEX idx_habits_direction ON habits(direction);
