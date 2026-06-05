package com.tracker.habit.habit.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateHabitRequest(String name, String description) {
}
