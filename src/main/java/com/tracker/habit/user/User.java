package com.tracker.habit.user;

import java.time.LocalDateTime;

public record User(
        Long id,
        String email,
        String passwordHash,
        LocalDateTime createdAt
) {
}
