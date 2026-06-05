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
        Habit habit = habitRepository.verifyOwnership(habitId, userId);
        return habitLogRepository.logToday(habit.id());
    }

    public void deleteLog(Long habitId, Long userId) {
        Habit habit = habitRepository.verifyOwnership(habitId, userId);
        habitLogRepository.deleteToday(habit.id());
    }

    public List<LocalDate> getAllHabitLogs(Long habitId, Long userId) {
        Habit habit = habitRepository.verifyOwnership(habitId, userId);
        List<HabitLog> logs = habitLogRepository.findAllByHabitId(habit.id());
        return logs.stream().map(HabitLog::completedOn).toList();
    }
}
