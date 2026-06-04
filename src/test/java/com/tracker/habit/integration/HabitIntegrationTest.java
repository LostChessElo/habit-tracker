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

class HabitIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    // A unique email prefix ensures no collision between tests even when
    // postgres persists data across the suite run in one JVM session
    private static int counter = 0;

    private String token;

    @BeforeEach
    void setUp() {
        String email = "habit_user_" + (++counter) + "@test.com";
        token = registerAndLogin(email, "password123");
    }


    //POST: /api/habits
    @Test
    void createHabit_validRequest_returns201WithHabit() {
        ResponseEntity<String> resp = post("/api/habits",
                Map.of("name", "Exercise", "description", "Daily workout"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parse(resp);
        assertThat(body.path("id").asLong()).isPositive();
        assertThat(body.path("name").asText()).isEqualTo("Exercise");
        assertThat(body.path("description").asText()).isEqualTo("Daily workout");
        assertThat(body.path("streak").asInt()).isZero();
        assertThat(body.path("completedToday").asBoolean()).isFalse();
    }

    @Test
    void createHabit_missingName_returns400() {
        ResponseEntity<String> resp = post("/api/habits",
                Map.of("description", "No name provided"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createHabit_noToken_returns401or403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/habits",
                new HttpEntity<>(Map.of("name", "Ghost"), headers),
                String.class
        );

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }


    // GET: /api/habits
    @Test
    void getAllHabits_noHabits_returnsEmptyList() {
        ResponseEntity<String> resp = get("/api/habits");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(parse(resp).isArray()).isTrue();
        assertThat(parse(resp).size()).isZero();
    }

    @Test
    void getAllHabits_withHabits_returnsOnlyOwnHabits() {
        post("/api/habits", Map.of("name", "Read", "description", "30 min"));
        post("/api/habits", Map.of("name", "Meditate", "description", "10 min"));

        // A second user creates their own habit should not appear in the first
        // user's list
        String otherToken = registerAndLogin("other_habit_" + counter + "@test.com", "pw");
        postWithToken(otherToken, "/api/habits", Map.of("name", "Other habit", "description", ""));

        ResponseEntity<String> resp = get("/api/habits");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = parse(resp);
        assertThat(list.size()).isEqualTo(2);
        list.forEach(h -> assertThat(h.path("name").asText()).isIn("Read", "Meditate"));
    }


    // GET: /api/habits/{id}
    @Test
    void getHabit_existingHabit_returns200() {
        long id = createHabit("Morning Run", "5k every day");

        ResponseEntity<String> resp = get("/api/habits/" + id);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(parse(resp).path("name").asText()).isEqualTo("Morning Run");
    }

    @Test
    void getHabit_nonExistentId_returns404() {
        ResponseEntity<String> resp = get("/api/habits/999999");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getHabit_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("owner_" + counter + "@test.com", "pw");
        long otherId = createHabitWithToken(otherToken, "Private habit", "");

        // The primary user should get 404
        ResponseEntity<String> resp = get("/api/habits/" + otherId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    // PUT: /api/habits/{id}
    @Test
    void updateHabit_validRequest_returns200WithUpdatedData() {
        long id = createHabit("Old Name", "Old description");

        ResponseEntity<String> resp = put("/api/habits/" + id,
                Map.of("name", "New Name", "description", "New description"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parse(resp);
        assertThat(body.path("name").asText()).isEqualTo("New Name");
        assertThat(body.path("description").asText()).isEqualTo("New description");
    }

    @Test
    void updateHabit_nonExistentId_returns404() {
        ResponseEntity<String> resp = put("/api/habits/999999",
                Map.of("name", "Irrelevant"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateHabit_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("update_owner_" + counter + "@test.com", "pw");
        long otherId = createHabitWithToken(otherToken, "Their habit", "");

        ResponseEntity<String> resp = put("/api/habits/" + otherId,
                Map.of("name", "Hijacked"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // DELETE: /api/habits/{id}
    @Test
    void deleteHabit_existingHabit_returns204AndIsGone() {
        long id = createHabit("To Delete", "");

        ResponseEntity<String> del = delete("/api/habits/" + id);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // A subsequent GET should return 404
        assertThat(get("/api/habits/" + id).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteHabit_nonExistentId_returns404() {
        ResponseEntity<String> resp = delete("/api/habits/999999");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteHabit_anotherUsersHabit_returns404() {
        String otherToken = registerAndLogin("del_owner_" + counter + "@test.com", "pw");
        long otherId = createHabitWithToken(otherToken, "Not yours", "");

        assertThat(delete("/api/habits/" + otherId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Confirm the habit exists for real owner
        assertThat(getWithToken(otherToken, "/api/habits/" + otherId).getStatusCode())
                .isEqualTo(HttpStatus.OK);
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
        ResponseEntity<String> resp = postWithToken(jwt, "/api/habits",
                Map.of("name", name, "description", description));
        return parse(resp).path("id").asLong();
    }

    private ResponseEntity<String> get(String path) {
        return getWithToken(token, path);
    }

    private ResponseEntity<String> getWithToken(String jwt, String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearerHeaders(jwt)), String.class);
    }

    private ResponseEntity<String> post(String path, Object body) {
        return postWithToken(token, path, body);
    }

    private ResponseEntity<String> postWithToken(String jwt, String path, Object body) {
        return rest.postForEntity(path, new HttpEntity<>(body, bearerHeaders(jwt)), String.class);
    }

    private ResponseEntity<String> put(String path, Object body) {
        return rest.exchange(path, HttpMethod.PUT,
                new HttpEntity<>(body, bearerHeaders(token)), String.class);
    }

    private ResponseEntity<String> delete(String path) {
        return rest.exchange(path, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)), String.class);
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