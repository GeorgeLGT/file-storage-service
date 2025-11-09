package ru.netology.filestorage.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.filestorage.dto.AuthRequest;
import ru.netology.filestorage.dto.AuthResponse;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileStorageIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.storage.path", () -> "./test-storage");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginAndAccessProtectedEndpoint() {
        AuthRequest authRequest = new AuthRequest("user@example.com", "password");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/cloud/login", authRequest, AuthResponse.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().getAuthToken());

        String authToken = loginResponse.getBody().getAuthToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> filesResponse = restTemplate.exchange(
                "/cloud/list", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, filesResponse.getStatusCode());
    }
}