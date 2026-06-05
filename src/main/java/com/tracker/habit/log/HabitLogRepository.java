package com.tracker.habit.log;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Data-access layer for the {@code habit_logs} table.
 *
 * <p>Each row records that a habit was completed on a specific date.
 * A unique constraint on {@code (habit_id, completed_on)} enforces
 * at-most-one log entry per habit per day, which the {@code ON CONFLICT DO NOTHING}
 * insert relies on.</p>
 */
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

    /**
     * Checks whether a habit has already been logged as complete today.
     *
     * @param habitId the habit to check
     * @return {@code true} if a log entry exists for today; {@code false} otherwise
     */
    public boolean isCompleteToday(Long habitId) {
        String query =  "SELECT COUNT(*) FROM habit_logs " +
                "WHERE habit_id = :habit_id AND completed_on = CURRENT_DATE";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id",habitId);
        Integer count = jdbcTemplate.queryForObject(query, params, Integer.class);
        return count != null && count > 0;
    }

    /**
     * Logs today's completion for a habit using an idempotent insert.
     *
     * <p>The {@code ON CONFLICT DO NOTHING} clause means calling this method
     * twice on the same day is safe, the second call simply does nothing and
     * returns {@code false}.</p>
     *
     * @param habitId the habit to log
     * @return {@code true} if a new row was inserted; {@code false} if today was already logged
     */
    public boolean logToday(Long habitId) {
        String query =  "INSERT INTO habit_logs (habit_id, completed_on) " +
                        "VALUES (:habit_id, CURRENT_DATE) " +
                        "ON CONFLICT DO NOTHING";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        return jdbcTemplate.update(query, params) > 0;
    }

    /**
     * Removes today's log entry for a habit, allowing it to be re-logged later.
     *
     * <p>If no entry exists for today this is a no-op.</p>
     *
     * @param habitId the habit whose today log should be removed
     */
    public void deleteToday(Long habitId) {
        String query = "DELETE FROM habit_logs " +
                "WHERE habit_id = :habit_id AND completed_on = CURRENT_DATE";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        jdbcTemplate.update(query, params);
    }

    /**
     * Returns all log entries for a habit across all time.
     *
     * @param habitId the habit whose history to retrieve
     * @return a list of {@link HabitLog} records; empty if the habit has never been logged
     */
    public List<HabitLog> findAllByHabitId(Long habitId) {
        String query = "SELECT id, habit_id, completed_on " +
                "FROM habit_logs WHERE habit_id = :habit_id";
        SqlParameterSource params = new MapSqlParameterSource().addValue("habit_id", habitId);
        return jdbcTemplate.query(query, params, habitLogRowMapper);
    }

    /**
     * Calculates the current consecutive-day streak for a habit.
     *
     * <p>The streak is the number of days in a row (counting backwards from
     * today or yesterday) that the habit was completed. The algorithm:</p>
     * <ol>
     *   <li>Fetches all completion dates in descending order.</li>
     *   <li>Returns 0 immediately if the most recent date is neither today nor yesterday
     *       (the streak has been broken).</li>
     *   <li>Walks backward from the starting date, incrementing the streak for each
     *       consecutive day, and stops on the first gap.</li>
     * </ol>
     *
     * @param habitId the habit to calculate the streak for
     * @return the current streak length in days; 0 if the habit has never been logged
     *         or the streak is broken
     */
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

        // Streak is broken if most recent day isnt today or yesterday
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
