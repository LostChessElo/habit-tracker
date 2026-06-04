package com.tracker.habit.log;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits/{id}/log")
public class HabitLogController {
    private final HabitLogService service;

    public HabitLogController(HabitLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LocalDate>> getAllHabitLogs(Authentication auth, @PathVariable Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getAllHabitLogs(habitId, userId));
    }

    @PostMapping
    public ResponseEntity<Void> markAsCompleted(Authentication auth, @PathVariable Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        Boolean marked = service.logHabit(habitId, userId);
        return marked ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLog(Authentication auth, @PathVariable Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        service.deleteLog(habitId, userId);
        return ResponseEntity.noContent().build();
    }
}
