package com.tracker.habit.log;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class HabitLogRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<HabitLog> habitLogRowMapper = (rs, rowNum) -> new HabitLog(
            rs.getLong("id"),
            rs.getLong("habit_id"),
            rs.getObject("completed_on", LocalDate.class)
    );

    public HabitLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbcTemplate = jdbc;
    }



    public boolean isCompleteToday(Long habitId) {
        String query =  "SELECT COUNT(*) FROM habit_logs " +
                "WHERE habit_id = :habit_id AND completed_on = CURRENT_DATE";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id",habitId);
        Integer count = jdbcTemplate.queryForObject(query, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean logToday(Long habitId) {
        String query =  "INSERT INTO habit_logs (habit_id, completed_on) " +
                        "VALUES (:habit_id, CURRENT_DATE) " +
                        "ON CONFLICT DO NOTHING";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        return jdbcTemplate.update(query, params) > 0;
    }

    public void deleteToday(Long habitId) {
        String query = "DELETE FROM habit_logs " +
                "WHERE habit_id = :habit_id AND completed_on = CURRENT_DATE";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        jdbcTemplate.update(query, params);
    }

    public List<HabitLog> findAllByHabitId(Long habitId) {
        String query = "SELECT id, habit_id, completed_on " +
                "FROM habit_logs WHERE habit_id = :habit_id";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        return jdbcTemplate.query(query, params, habitLogRowMapper);
    }

    public int calculateStreak(Long habitId) {
        int streak = 0;
        // fetch all completion dates and order them in descending order
        String query = "SELECT completed_on FROM habit_logs " +
                "WHERE habit_id = :habit_id " +
                "ORDER BY completed_on DESC";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        // row mapper to map each return to a localdate object
        RowMapper<LocalDate> dateRowMapper = (rs, rowNum) -> rs.getObject("completed_on", LocalDate.class);
        List<LocalDate> dates = jdbcTemplate.query(query, params, dateRowMapper);
        if (dates.isEmpty()) {
            return streak;
        }

        LocalDate mostRecentDate = dates.getFirst();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate expected = today;

        if (!mostRecentDate.isEqual(today) && !mostRecentDate.isEqual(yesterday)) {
            return streak;
        }

        for (LocalDate date: dates) {
            if (date.isEqual(expected)) {
                expected = expected.minusDays(1);
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
