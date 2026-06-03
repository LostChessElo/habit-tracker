package com.tracker.habit.habit;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.habit.dtos.HabitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HabitService {
    private final HabitRepository habitRepository;
    // needs to inject the HabitLogRepository

    public HabitService(HabitRepository repository) {
        this.habitRepository = repository;
    }

    public HabitResponse createHabit(String name, String description, Long uId) {
        Long habitId = habitRepository.save(name, description, uId); // save habit
        Habit habit = habitRepository.findById(habitId).orElseThrow(); // fetch habit
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
        // fetch a habit with its id after verifyOwnership call
        // call habitRepository.findById()
        // need to calculate streak and fetch completedOn
    }

    public List<HabitResponse> getAllHabits(Long userId) {
        // needs to return a list of all habits belonging to this user
        // call habit repo getall
        // need to calculate streak and fetch completedOn
    }

    public HabitResponse updateHabit(Long habitId, Long userid, String name, String description) {
        //verify ownership
        // call update habit in the repo
        // if the name field is null just use the habits original name - when a user
        // only wants to update description
    }

    public void deleteHabit(Long habitId, Long userId) {
        // verify ownership -> call repository.delete()
    }


    private Habit verifyOwnership(Long habitId, Long userId) {
        Optional<Habit> habit = habitRepository.findById(habitId);
        if (!habit.isPresent() || !habit.get().userId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Habit not found.");
        }
        return habit.get();
    }
}
