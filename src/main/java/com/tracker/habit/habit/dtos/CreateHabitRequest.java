package com.tracker.habit.habit.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateHabitRequest(@NotBlank String name, String description) {
}
