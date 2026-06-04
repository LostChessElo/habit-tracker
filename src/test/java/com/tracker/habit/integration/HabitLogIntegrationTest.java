package com.tracker.habit.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HabitLogIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    private static int counter = 0;

    private String token;
    private long habitId;

    @BeforeEach
    void setUp() {
        String email = "log_user_" + (++counter) + "@test.com";
        token = registerAndLogin(email, "password123");
        habitId = createHabit("Test Habit", "For log tests");
    }

    // POST: /api/habits/{id}/log
    @Test
    void logHabit_firstTimeToday_returns200() {
        ResponseEntity<String> resp = postLog(habitId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logHabit_duplicateToday_returns409() {
        postLog(habitId); // first log

        ResponseEntity<String> second = postLog(habitId);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void logHabit_nonExistentHabit_returns404() {
        ResponseEntity<String> resp = postLog(999999L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void logHabit_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("log_owner_" + counter + "@test.com", "pw");
        long otherHabit = createHabitWithToken(otherToken, "Other", "");

        ResponseEntity<String> resp = postLog(otherHabit);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void logHabit_noToken_returns401or403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/habits/" + habitId + "/log",
                new HttpEntity<>(null, headers),
                String.class
        );

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    // GET: /api/habits/{id}/log
    @Test
    void getLogs_noLogs_returnsEmptyList() {
        ResponseEntity<String> resp = getLogs(habitId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = parse(resp);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isZero();
    }

    @Test
    void getLogs_afterLogging_returnsTodayDate() {
        postLog(habitId);

        ResponseEntity<String> resp = getLogs(habitId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = parse(resp);
        assertThat(list.size()).isEqualTo(1);
        // The date should be today in ISO format
        assertThat(list.get(0).asText()).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void getLogs_nonExistentHabit_returns404() {
        assertThat(getLogs(999999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getLogs_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("getlogs_owner_" + counter + "@test.com", "pw");
        long otherHabit = createHabitWithToken(otherToken, "Private", "");

        assertThat(getLogs(otherHabit).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // DELETE: /api/habits/{id}/log
    @Test
    void deleteLog_afterLogging_returns204AndListIsEmpty() {
        postLog(habitId);

        ResponseEntity<String> del = deleteLog(habitId);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(parse(getLogs(habitId)).size()).isZero();
    }

    @Test
    void deleteLog_whenNothingLogged_returns204() {
        // Idempotent — deleting a non-existent log for today is harmless
        ResponseEntity<String> resp = deleteLog(habitId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteLog_nonExistentHabit_returns404() {
        assertThat(deleteLog(999999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteLog_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("dellog_owner_" + counter + "@test.com", "pw");
        long otherHabit = createHabitWithToken(otherToken, "Private", "");

        assertThat(deleteLog(otherHabit).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Streak alg
    @Test
    void getHabit_afterLogging_completedTodayIsTrue() {
        postLog(habitId);

        JsonNode habit = parse(getHabit(habitId));
        System.out.println(getHabit(habitId).getBody());
        assertThat(habit.path("completed").asBoolean()).isTrue();
    }

    @Test
    void getHabit_afterLoggingAndDeleting_completedTodayIsFalse() {
        postLog(habitId);
        deleteLog(habitId);

        JsonNode habit = parse(getHabit(habitId));

        assertThat(habit.path("completedToday").asBoolean()).isFalse();
    }

    @Test
    void getHabit_afterLogging_streakIsAtLeastOne() {
        postLog(habitId);

        JsonNode habit = parse(getHabit(habitId));

        assertThat(habit.path("streak").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getHabit_withNoLogs_streakIsZero() {
        JsonNode habit = parse(getHabit(habitId));

        assertThat(habit.path("streak").asInt()).isZero();
    }


    private String registerAndLogin(String email, String password) {
        post_noAuth("/api/auth/register", Map.of("email", email, "password", password));
        ResponseEntity<String> login = post_noAuth("/api/auth/login",
                Map.of("email", email, "password", password));
        try {
            return mapper.readTree(login.getBody()).path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JWT", e);
        }
    }

    private long createHabit(String name, String description) {
        return createHabitWithToken(token, name, description);
    }

    private long createHabitWithToken(String jwt, String name, String description) {
        ResponseEntity<String> resp = rest.postForEntity("/api/habits",
                new HttpEntity<>(Map.of("name", name, "description", description), bearerHeaders(jwt)),
                String.class);
        return parse(resp).path("id").asLong();
    }

    private ResponseEntity<String> postLog(Long id) {
        return rest.postForEntity("/api/habits/" + id + "/log",
                new HttpEntity<>(null, bearerHeaders(token)), String.class);
    }

    private ResponseEntity<String> getLogs(Long id) {
        return rest.exchange("/api/habits/" + id + "/log",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), String.class);
    }

    private ResponseEntity<String> deleteLog(Long id) {
        return rest.exchange("/api/habits/" + id + "/log",
                HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(token)), String.class);
    }

    private ResponseEntity<String> getHabit(Long id) {
        return rest.exchange("/api/habits/" + id,
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), String.class);
    }

    private ResponseEntity<String> post_noAuth(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders bearerHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);
        return headers;
    }

    private JsonNode parse(ResponseEntity<String> resp) {
        try {
            return mapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body: " + resp.getBody(), e);
        }
    }
}