package com.tracker.habit.habit.dtos;

import java.time.LocalDateTime;

public record HabitResponse(
        Long id,
        String name,
        String description,
        int streak,
        boolean completed,
        LocalDateTime createdAt
) {
}
