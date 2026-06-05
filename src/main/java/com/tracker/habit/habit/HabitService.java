package com.tracker.habit.habit;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.habit.dtos.HabitResponse;
import com.tracker.habit.log.HabitLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for habit management.
 *
 * <p>All mutating operations verify that the requesting user owns the target
 * habit before proceeding. A {@link HabitResponse} is always enriched with
 * live streak and completion data fetched from {@link HabitLogRepository}.</p>
 */
@Service
public class HabitService {
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    public HabitService(HabitRepository repository, HabitLogRepository habitLogRepository) {
        this.habitRepository = repository;
        this.habitLogRepository = habitLogRepository;
    }

    /**
     * Creates a new habit for the given user.
     *
     * <p>A freshly created habit has a streak of 0 and is not yet completed today.</p>
     *
     * @param name        the display name of the habit
     * @param description an optional description of the habit
     * @param uId         the ID of the owning user
     * @return the created habit as a {@link HabitResponse}
     */
    public HabitResponse createHabit(String name, String description, Long uId) {
        Habit habit = habitRepository.save(name, description, uId); // save habit
        return new HabitResponse(
                habit.id(),
                habit.name(),
                habit.description(),
                0,
                false,
                habit.createdAt()
        );
    }

    /**
     * Retrieves a single habit by ID, enriched with current streak and today's completion status.
     *
     * @param habitId the ID of the habit to retrieve
     * @param userId  the ID of the requesting user; must match the habit owner
     * @return the habit as a {@link HabitResponse} with live streak data
     * @throws ApiException with {@code 404 NOT_FOUND} if the habit does not exist or is not owned by the user
     */
    public HabitResponse getHabit(Long habitId, Long userId) {
        Habit habit = verifyOwnership(habitId, userId);
        int streak = habitLogRepository.calculateStreak(habitId);
        return new HabitResponse(
                habit.id(),
                habit.name(),
                habit.description(),
                streak,
                habitLogRepository.isCompleteToday(habitId),
                habit.createdAt()
        );
    }

    /**
     * Returns all habits belonging to the given user, each enriched with streak and completion data.
     *
     * @param userId the ID of the user whose habits to retrieve
     * @return a list of {@link HabitResponse} objects; empty if the user has no habits
     */
    public List<HabitResponse> getAllHabits(Long userId) {
        List<Habit> habits = habitRepository.findAllByUid(userId);
        return habits.stream()
                .map(habit -> new HabitResponse(
                        habit.id(),
                        habit.name(),
                        habit.description(),
                        habitLogRepository.calculateStreak(habit.id()),
                        habitLogRepository.isCompleteToday(habit.id()),
                        habit.createdAt()
                )).toList();
    }

    /**
     * Updates the name and/or description of an existing habit.
     *
     * <p>Null fields are treated as "no changes", in this case the existing value is kept.</p>
     *
     * @param habitId     the ID of the habit to update
     * @param userid      the ID of the requesting user; must match the habit owner
     * @param name        the new name, or {@code null} to keep the current value
     * @param description the new description, or {@code null} to keep the current value
     * @return the updated habit as a {@link HabitResponse} with live streak data
     * @throws ApiException with {@code 404 NOT_FOUND} if the habit does not exist or is not owned by the user
     */
    @Transactional
    public HabitResponse updateHabit(Long habitId, Long userid, String name, String description) {
        Habit habit = verifyOwnership(habitId, userid);
        String newName = name == null ? habit.name() : name;
        String newDescription = description == null ? habit.description() : description;
        habitRepository.updateNameAndDescription(habitId, newName, newDescription);
        return new HabitResponse(
                habit.id(),
                newName,
                newDescription,
                habitLogRepository.calculateStreak(habit.id()),
                habitLogRepository.isCompleteToday(habit.id()),
                habit.createdAt()
        );
    }

    /**
     * Deletes a habit and all its associated log entries.
     *
     * @param habitId the ID of the habit to delete
     * @param userId  the ID of the requesting user; must match the habit owner
     * @throws ApiException with {@code 404 NOT_FOUND} if the habit does not exist or is not owned by the user
     */
    public void deleteHabit(Long habitId, Long userId) {
        verifyOwnership(habitId, userId);
        habitRepository.deleteById(habitId);
    }

    /**
     * Loads a habit and asserts that it belongs to the specified user.
     *
     * <p>Ownership failures and missing habits both return a {@code 404} to
     * avoid leaking the existence of habits owned by other users.</p>
     *
     * @param habitId the ID of the habit to load
     * @param userId  the ID of the expected owner
     * @return the {@link Habit} record if found and owned by {@code userId}
     * @throws ApiException with {@code 404 NOT_FOUND} if the habit is missing or owned by someone else
     */
    public Habit verifyOwnership(Long habitId, Long userId) {
        Optional<Habit> habit = habitRepository.findById(habitId);
        if (habit.isEmpty() || !habit.get().userId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Habit not found.");
        }
        return habit.get();
    }
}
