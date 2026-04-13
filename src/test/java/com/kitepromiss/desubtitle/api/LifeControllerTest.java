package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LifeControllerTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = tolerantRestTemplate();

    private static RestTemplate tolerantRestTemplate() {
        RestTemplate t = new RestTemplate();
        t.setErrorHandler(
                new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) {
                        return false;
                    }
                });
        return t;
    }
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void lifeWithoutAuthorizationReturns400() {
        ResponseEntity<String> r = restTemplate.getForEntity(baseUrl() + "/life", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().contains("missing_token"));
    }

    @Test
    void lifeWithNonBearerAuthorizationReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic dGVzdA==");
        ResponseEntity<String> r =
                restTemplate.exchange(baseUrl() + "/life", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().contains("invalid_authorization_header"));
    }

    @Test
    void lifeWithValidTokenReturnsPayloadWithoutRefresh() throws Exception {
        String jwt = fetchNewToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        ResponseEntity<String> r =
                restTemplate.exchange(baseUrl() + "/life", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        JsonNode n = jsonMapper.readTree(r.getBody());
        assertTrue(n.path("alive").booleanValue());
        assertTrue(n.path("submittedTokenValid").booleanValue());
        assertFalse(n.path("tokenRefreshed").booleanValue());
        assertEquals(jwt, n.path("token").asText());
        assertTrue(n.path("userId").isTextual());
        assertEquals(3, n.path("videoProcessingLanes").intValue());
        assertTrue(n.has("indicators"));
        assertTrue(n.path("indicators").path("capturedAtEpochMillis").isIntegralNumber());
        double online = n.path("indicators").path("gauges").path("online_users").doubleValue();
        assertTrue(online >= 1.0, "online_users should include this life ping");
    }

    @Test
    void lifeWithInvalidTokenRefreshes() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("eyJhbGciOiJIUzI1NiJ9.e30.invalid");
        ResponseEntity<String> r =
                restTemplate.exchange(baseUrl() + "/life", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        JsonNode n = jsonMapper.readTree(r.getBody());
        assertTrue(n.path("alive").booleanValue());
        assertFalse(n.path("submittedTokenValid").booleanValue());
        assertTrue(n.path("tokenRefreshed").booleanValue());
        assertTrue(n.path("token").asText().length() > 20);
        assertEquals(3, n.path("videoProcessingLanes").intValue());
        assertTrue(n.has("indicators"));
        double online = n.path("indicators").path("gauges").path("online_users").doubleValue();
        assertTrue(online >= 1.0, "online_users should include life ping after refresh");
    }

    private String fetchNewToken() throws Exception {
        ResponseEntity<String> tokenRes = restTemplate.getForEntity(baseUrl() + "/getUserToken", String.class);
        assertTrue(tokenRes.getStatusCode().is2xxSuccessful(), tokenRes.getBody());
        JsonNode root = jsonMapper.readTree(tokenRes.getBody());
        String jwt = root.path("token").asText();
        assertFalse(jwt.isEmpty());
        return jwt;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
