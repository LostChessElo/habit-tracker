package com.tracker.habit.habit;

import com.tracker.habit.habit.dtos.CreateHabitRequest;
import com.tracker.habit.habit.dtos.HabitResponse;
import com.tracker.habit.habit.dtos.UpdateHabitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {
    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<HabitResponse>> getAllHabits(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getAllHabits(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitResponse> getHabit(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getHabit(habitId, userId));
    }

    @PostMapping
    public ResponseEntity<HabitResponse> createHabit(Authentication auth, @RequestBody @Valid CreateHabitRequest createHabitRequest) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createHabit(
                        createHabitRequest.name(),
                        createHabitRequest.description(),
                        userId
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitResponse> updateHabit(Authentication auth, @PathVariable("id") Long habitId, @RequestBody UpdateHabitRequest updateHabitRequest) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.updateHabit(
                habitId,
                userId,
                updateHabitRequest.name(),
                updateHabitRequest.description()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        service.deleteHabit(habitId,userId);
        return ResponseEntity.noContent().build();
    }
}
