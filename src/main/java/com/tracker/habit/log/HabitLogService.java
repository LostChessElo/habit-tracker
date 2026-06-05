package com.tracker.habit.log;

import com.tracker.habit.habit.Habit;
import com.tracker.habit.habit.HabitService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for habit log operations (check-ins and history).
 *
 * <p>Delegates ownership verification to {@link HabitService} so that a
 * user can only log, unlog, or read history for habits they own.</p>
 */
@Service
public class HabitLogService {
    private final HabitLogRepository habitLogRepository;
    private final HabitService habitService;

    public HabitLogService(HabitLogRepository habitLogRepository, HabitService service) {
        this.habitLogRepository = habitLogRepository;
        this.habitService = service;
    }

    /**
     * Marks a habit as complete for today.
     *
     * <p>The operation is idempotent at the repository level, logging the
     * same habit twice on the same day is safe, but the return value will be
     * {@code false} on the duplicate attempt.</p>
     *
     * @param habitId the ID of the habit to log
     * @param userId  the ID of the requesting user; must own the habit
     * @return {@code true} if a new log entry was created; {@code false} if already logged today
     * @throws com.tracker.habit.exception.ApiException with {@code 404 NOT_FOUND} if the habit
     *         does not exist or is not owned by the user
     */
    public boolean logHabit(Long habitId, Long userId) {
        Habit habit = habitService.verifyOwnership(habitId, userId);
        return habitLogRepository.logToday(habit.id());
    }


    /**
     * Removes today's completion log for a habit (undo check-in).
     *
     * <p>If the habit has not been logged today this is a no-op.</p>
     *
     * @param habitId the ID of the habit to unlog
     * @param userId  the ID of the requesting user; must own the habit
     * @throws com.tracker.habit.exception.ApiException with {@code 404 NOT_FOUND} if the habit
     *         does not exist or is not owned by the user
     */
    public void deleteLog(Long habitId, Long userId) {
        Habit habit = habitService.verifyOwnership(habitId, userId);
        habitLogRepository.deleteToday(habit.id());
    }

    /**
     * Returns the full log history for a habit as a list of completion dates.
     *
     * @param habitId the ID of the habit
     * @param userId  the ID of the requesting user; must own the habit
     * @return a list of {@link LocalDate} values representing every day the habit was completed;
     *         empty if the habit has never been logged
     * @throws com.tracker.habit.exception.ApiException with {@code 404 NOT_FOUND} if the habit
     *         does not exist or is not owned by the user
     */
    public List<LocalDate> getAllHabitLogs(Long habitId, Long userId) {
        Habit habit = habitService.verifyOwnership(habitId, userId);
        List<HabitLog> logs = habitLogRepository.findAllByHabitId(habit.id());
        return logs.stream().map(HabitLog::completedOn).toList();
    }
}
