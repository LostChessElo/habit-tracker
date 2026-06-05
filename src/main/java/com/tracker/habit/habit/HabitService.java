package com.tracker.habit.habit;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.habit.dtos.HabitResponse;
import com.tracker.habit.log.HabitLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HabitService {
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    public HabitService(HabitRepository repository, HabitLogRepository habitLogRepository) {
        this.habitRepository = repository;
        this.habitLogRepository = habitLogRepository;
    }

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

    public void deleteHabit(Long habitId, Long userId) {
        verifyOwnership(habitId, userId);
        habitRepository.deleteById(habitId);
    }

    public Habit verifyOwnership(Long habitId, Long userId) {
        Optional<Habit> habit = habitRepository.findById(habitId);
        if (habit.isEmpty() || !habit.get().userId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Habit not found.");
        }
        return habit.get();
    }
}
