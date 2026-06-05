package com.tracker.habit.habit;

import com.tracker.habit.exception.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for the {@code habits} table.
 *
 * <p>All queries use {@link NamedParameterJdbcTemplate} with named parameters
 * to prevent SQL injection. Results are mapped to {@link Habit} records via
 * a shared {@link RowMapper}.</p>
 */
@Repository
public class HabitRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Habit> habitRowMapper = (rs, rowNum) -> new Habit(
            rs.getObject("id", Long.class),
            rs.getObject("user_id", Long.class),
            rs.getString("name"),
            rs.getString("description"),
            rs.getObject("created_at", LocalDateTime.class)
    );

    public HabitRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts a new habit and returns the persisted record.
     *
     * <p>Uses a {@code RETURNING} clause so the database-generated ID and
     * timestamp are available without a second query.</p>
     *
     * @param name        the habit name
     * @param description the habit description
     * @param userId      the owning user's ID
     * @return the newly created {@link Habit} record including its generated ID and timestamp
     */
    public Habit save(String name, String description, Long userId) {
        String query = "INSERT INTO habits (name, description, user_id) " +
                "VALUES (:name, :description, :user_id) " +
                "RETURNING id, name, description, user_id, created_at";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("description", description)
                .addValue("user_id",userId);
        return jdbcTemplate.queryForObject(query, parameterSource, habitRowMapper);
    }

    /**
     * Looks up a habit by its primary key.
     *
     * @param id the habit ID
     * @return an {@link Optional} containing the habit, or empty if not found
     */
    public Optional<Habit> findById(Long id) {
        String query = "SELECT id, user_id, name, description, created_at " +
                 "FROM habits WHERE id = :id";
        SqlParameterSource param = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(query, param, habitRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns all habits owned by a given user.
     *
     * @param userId the user whose habits to retrieve
     * @return a list of matching {@link Habit} records; empty if the user has no habits
     */
    public List<Habit> findAllByUid(Long userId) {
        String query = "SELECT id, user_id, name, description, created_at " +
                "FROM habits WHERE user_id = :user_id";
        SqlParameterSource parameterSource = new MapSqlParameterSource().addValue("user_id",userId);
        return jdbcTemplate.query(query, parameterSource, habitRowMapper);
    }

    /**
     * Deletes a habit by its primary key.
     *
     * <p>Cascading deletes in the database schema remove any associated
     * {@code habit_logs} rows automatically.</p>
     *
     * @param id the ID of the habit to delete
     */
    public void deleteById(Long id) {
        String query = "DELETE FROM habits WHERE id = :id";
        SqlParameterSource parameterSource = new MapSqlParameterSource().addValue("id", id);
        jdbcTemplate.update(query, parameterSource);
    }

    /**
     * Updates the name and description of an existing habit.
     *
     * @param id          the ID of the habit to update
     * @param name        the new name
     * @param description the new description
     */
    public void updateNameAndDescription(Long id,String name,String description) {
        String query = "UPDATE habits " +
                "SET description = :description, name = :name " +
                "WHERE id = :id";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("description", description)
                .addValue("name", name)
                .addValue("id", id);
        jdbcTemplate.update(query,parameterSource);
    }
}
