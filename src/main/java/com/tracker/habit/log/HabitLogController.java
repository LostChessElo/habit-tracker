package com.tracker.habit.log;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for daily habit check-in and log history.
 *
 * <p>All endpoints require a valid JWT. The user ID is sourced from the
 * JWT principal, never from the request body.</p>
 *
 * <p>Base path: {@code /api/habits/{id}/log}</p>
 */
@RestController
@RequestMapping("/api/habits/{id}/log")
public class HabitLogController {
    private final HabitLogService service;

    public HabitLogController(HabitLogService service) {
        this.service = service;
    }

    /**
     * Returns the full completion history for a habit as a list of dates.
     *
     * @param auth    the security context; principal is the user ID
     * @param habitId the ID of the habit
     * @return {@code 200 OK} with a list of {@link LocalDate} values (may be empty)
     */
    @GetMapping
    public ResponseEntity<List<LocalDate>> getAllHabitLogs(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getAllHabitLogs(habitId, userId));
    }

    /**
     * Marks a habit as complete for today.
     *
     * <p>Returns {@code 409 CONFLICT} if the habit is already logged today,
     * allowing clients to update their UI without an additional fetch.</p>
     *
     * @param auth    the security context; principal is the user ID
     * @param habitId the ID of the habit to check in
     * @return {@code 200 OK} if the log was created; {@code 409 CONFLICT} if already logged today
     */
    @PostMapping
    public ResponseEntity<Void> markAsCompleted(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        boolean marked = service.logHabit(habitId, userId);
        return marked ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /**
     * Removes today's log entry for a habit (undo check-in).
     *
     * <p>If the habit has not been logged today this is a no-op and still
     * returns {@code 204}.</p>
     *
     * @param auth    the security context; principal is the user ID
     * @param habitId the ID of the habit to unlog
     * @return {@code 204 NO CONTENT}
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteLog(Authentication auth, @PathVariable("id") Long habitId) {
        Long userId = (Long) auth.getPrincipal();
        service.deleteLog(habitId, userId);
        return ResponseEntity.noContent().build();
    }
}
