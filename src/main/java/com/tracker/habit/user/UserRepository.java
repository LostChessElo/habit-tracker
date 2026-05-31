package com.tracker.habit.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// CRUD operations
@Repository
public class UserRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbcTemplate = jdbc;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
        rs.getObject("id", Long.class),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getObject("created_at", LocalDateTime.class)
    );

    // Read a single user
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, created_at " +
                     "FROM users WHERE email = :email";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, userRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // Create user
    public Long save(String email, String passwordHash) {
        String sql = "INSERT INTO users (email, password_hash) " +
                     "VALUES (:email, :passwordHash) " +
                     "RETURNING id";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("passwordHash", passwordHash);
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    //Delete user
    public void deleteUserByEmail(String email) {
        String query = "DELETE FROM users WHERE email = :email";
        SqlParameterSource params = new MapSqlParameterSource().addValue("email", email);
        jdbcTemplate.update(query, params);
    }

    // Read: All users
    public List<User> findAll() {
        String query = "SELECT id, email, password_hash, created_at FROM users";
        return jdbcTemplate.query(query, userRowMapper);
    }
}
