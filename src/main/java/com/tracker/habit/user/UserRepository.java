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

/**
 * Data-access layer for the {@code users} table.
 *
 * <p>Passwords are never stored in plaintext — the {@code passwordHash} parameter
 * on {@link #save} expects a BCrypt-hashed value produced by the caller.</p>
 */
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

    /**
     * Looks up a user by their email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user, or empty if no account exists for that email
     */
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

    /**
     * Inserts a new user and returns the generated ID.
     *
     * <p>The {@code passwordHash} must already be BCrypt-encoded.</p>
     *
     * @param email        the user's email address; must be unique
     * @param passwordHash the BCrypt hash of the user's password
     * @return the database-generated ID of the newly created user
     */
    public Long save(String email, String passwordHash) {
        String sql = "INSERT INTO users (email, password_hash) " +
                     "VALUES (:email, :passwordHash) " +
                     "RETURNING id";
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("passwordHash", passwordHash);
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    /**
     * Deletes a user by their email address.
     *
     * <p>Used primarily in integration tests to clean up test data.
     * If no user exists with the given email this is a no-op.</p>
     *
     * @param email the email address of the user to delete
     */
    public void deleteUserByEmail(String email) {
        String query = "DELETE FROM users WHERE email = :email";
        SqlParameterSource params = new MapSqlParameterSource().addValue("email", email);
        jdbcTemplate.update(query, params);
    }

    /**
     * Returns every user in the database.
     *
     * <p>Intended for administrative or testing purposes only, not exposed via any API endpoint.</p>
     *
     * @return a list of all {@link User} records; empty if no users exist
     */
    public List<User> findAll() {
        String query = "SELECT id, email, password_hash, created_at FROM users";
        return jdbcTemplate.query(query, userRowMapper);
    }
}
