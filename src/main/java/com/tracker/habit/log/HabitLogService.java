package com.tracker.habit.log;


import com.tracker.habit.exception.ApiException;
import com.tracker.habit.habit.Habit;
import com.tracker.habit.habit.HabitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HabitLogService {
    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;

    public HabitLogService(HabitLogRepository habitLogRepository, HabitRepository habitRepository) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
    }

    public boolean logHabit(Long habitId, Long userId) {
        Habit habit = verifyOwnership(habitId, userId);
        return habitLogRepository.logToday(habit.id());
    }

    public void deleteLog(Long habitId, Long userId) {
        Habit habit = verifyOwnership(habitId, userId);
        habitLogRepository.deleteToday(habit.id());
    }

    public List<LocalDate> getAllHabitLogs(Long habitId, Long userId) {
        Habit habit = verifyOwnership(habitId, userId);
        List<HabitLog> logs = habitLogRepository.findAllByHabitId(habit.id());
        return logs.stream().map(HabitLog::completedOn).toList();
    }

    private Habit verifyOwnership(Long habitId, Long userId) {
        Optional<Habit> habit = habitRepository.findById(habitId);
        if (!habit.isPresent() || !habit.get().userId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Habit not found.");
        }
        return habit.get();
    }
}
