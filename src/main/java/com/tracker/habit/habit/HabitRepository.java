package com.tracker.habit.habit;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // CREATE:
    public Long save(String name, String description, Long userId) {
        String query = "INSERT INTO habits (name, description, user_id) " +
                "VALUES (:name, :description, :user_id) " +
                "RETURNING id";
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("description", description)
                .addValue("user_id",userId);
        return jdbcTemplate.queryForObject(query, parameterSource, Long.class);
    }

    // READ
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

    public List<Habit> findAllByUid(Long userId) {
        String query = "SELECT id, user_id, name, description, created_at " +
                "FROM habits WHERE user_id = :user_id";
        SqlParameterSource parameterSource = new MapSqlParameterSource().addValue("user_id",userId);
        return jdbcTemplate.query(query, parameterSource, habitRowMapper);
    }

    // DELETE
    public void deleteById(Long id) {
        String query = "DELETE FROM habits WHERE id = :id";
        SqlParameterSource parameterSource = new MapSqlParameterSource().addValue("id", id);
        jdbcTemplate.update(query, parameterSource);
    }

    // UPDATE:
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
