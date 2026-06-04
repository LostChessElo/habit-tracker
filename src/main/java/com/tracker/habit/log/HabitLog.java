package com.tracker.habit.log;

import java.time.LocalDate;

public record HabitLog(
        Long id,
        Long habit_id,
        LocalDate completedOn
) {
}
