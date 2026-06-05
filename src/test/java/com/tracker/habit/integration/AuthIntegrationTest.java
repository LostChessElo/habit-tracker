package com.tracker.habit.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    private static int counter = 0;

    //api/auth/register
    @Test
    void register_validRequest_returns201WithToken() {
        var body = Map.of("email", "register_new_" + (++counter) + "@test.com",
                "password", "secret123");

        ResponseEntity<String> resp = post("/api/auth/register", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(tokenFrom(resp)).isNotBlank();
    }

    @Test
    void register_duplicateEmail_returns409() {
        String email = "register_dup_" + (++counter) + "@test.com";
        var body = Map.of("email", email, "password", "secret123");

        post("/api/auth/register", body);
        ResponseEntity<String> second = post("/api/auth/register", body);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_missingEmail_returns400() {
        var body = Map.of("password", "secret123");

        ResponseEntity<String> resp = post("/api/auth/register", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_missingPassword_returns400() {
        var body = Map.of("email", "nopwd_" + (++counter) + "@test.com");

        ResponseEntity<String> resp = post("/api/auth/register", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_invalidEmailFormat_returns400() {
        var body = Map.of("email", "not-an-email-" + (++counter),
                "password", "secret123");

        ResponseEntity<String> resp = post("/api/auth/register", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    //api/auth/login
    @Test
    void login_validCredentials_returns200WithToken() {
        String email = "login_valid_" + (++counter) + "@test.com";
        var body = Map.of("email", email, "password", "secret123");
        post("/api/auth/register", body);

        ResponseEntity<String> resp = post("/api/auth/login", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tokenFrom(resp)).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        String email = "login_wrongpwd_" + (++counter) + "@test.com";
        post("/api/auth/register", Map.of("email", email, "password", "correct"));

        ResponseEntity<String> resp = post("/api/auth/login",
                Map.of("email", email, "password", "wrong"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unregisteredEmail_returns401() {
        var body = Map.of("email", "ghost_" + (++counter) + "@test.com",
                "password", "whatever");

        ResponseEntity<String> resp = post("/api/auth/login", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_missingPassword_returns400() {
        var body = Map.of("email", "missing_pwd_" + (++counter) + "@test.com");

        ResponseEntity<String> resp = post("/api/auth/login", body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    private ResponseEntity<String> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private String tokenFrom(ResponseEntity<String> resp) {
        try {
            JsonNode node = mapper.readTree(resp.getBody());
            return node.path("token").asText();
        } catch (Exception e) {
            return "";
        }
    }
}