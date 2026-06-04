package com.tracker.habit.habit;

import com.tracker.habit.habit.dtos.CreateHabitRequest;
import com.tracker.habit.habit.dtos.HabitResponse;
import com.tracker.habit.habit.dtos.UpdateHabitRequest;
import jakarta.validation.Valid;
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

    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitResponse> getHabit(Authentication auth, @PathVariable Long habitId) {

    }

    @PostMapping
    public ResponseEntity<HabitResponse> createHabit(Authentication auth, @RequestBody @Valid CreateHabitRequest createHabitRequest) {

    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitResponse> updateHabit(Authentication auth, @PathVariable @RequestBody @Valid UpdateHabitRequest updateHabitRequest) {

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(Authentication auth, @PathVariable Long habitId) {

    }
}
