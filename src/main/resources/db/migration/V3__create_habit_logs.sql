CREATE TABLE habit_logs (
    id           BIGSERIAL PRIMARY KEY,
    habit_id     BIGINT NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    completed_on DATE NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT uq_habit_log UNIQUE (habit_id, completed_on)
);