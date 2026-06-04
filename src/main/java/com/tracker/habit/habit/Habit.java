package com.tracker.habit.habit;

import java.time.LocalDateTime;

public record Habit(
        Long id,
        Long userId,
        String name,
        String description,
        LocalDateTime createdAt
) {
}
