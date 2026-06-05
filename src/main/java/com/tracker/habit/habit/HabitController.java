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

/**
 * REST controller for habit CRUD operations.
 *
 * <p>All endpoints require a valid JWT. The authenticated user's ID is
 * extracted from the {@link Authentication} principal (set by
 * {@code JwtAuthFilter}) and never sourced from the request body.</p>
 *
 * <p>Base path: {@code /api/habits}</p>
 */
@RestController
@RequestMapping("/api/habits")
public class HabitController {
    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    /**
     * Returns all habits belonging to the authenticated user.
     *
     * @param auth the security context; principal is the user ID
     * @return {@code 200 OK} with a list of {@link HabitResponse} objects (may be empty)
     */
    @GetMapping
    public ResponseEntity<List<HabitResponse>> getAllHabits(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getAllHabits(userId));
    }

    /**
     * Returns a single habit by ID, including streak and today's completion status.
     *
     * @param auth    the security context; principal is the user ID
     * @param habitId the ID of the habit to retrieve
     * @return {@code 200 OK} with the {@link HabitResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<HabitResponse> getHabit(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getHabit(habitId, userId));
    }

    /**
     * Creates a new habit for the authenticated user.
     *
     * @param auth               the security context; principal is the user ID
     * @param createHabitRequest the request body containing {@code name} and optional {@code description}
     * @return {@code 201 CREATED} with the newly created {@link HabitResponse}
     */
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

    /**
     * Updates the name and/or description of an existing habit.
     *
     * <p>Fields omitted from the request body (null) are left unchanged.</p>
     *
     * @param auth               the security context; principal is the user ID
     * @param habitId            the ID of the habit to update
     * @param updateHabitRequest the request body with optional {@code name} and {@code description}
     * @return {@code 200 OK} with the updated {@link HabitResponse}
     */
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

    /**
     * Deletes a habit and all its log history.
     *
     * @param auth    the security context; principal is the user ID
     * @param habitId the ID of the habit to delete
     * @return {@code 204 NO CONTENT}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        service.deleteHabit(habitId,userId);
        return ResponseEntity.noContent().build();
    }
}
