package com.kitepromiss.desubtitle.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebDirectoryResourceConfigTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void rootReturnsIndexHtml() {
        ResponseEntity<String> r = restTemplate.getForEntity(baseUrl() + "/", String.class);
        assertTrue(r.getStatusCode().is2xxSuccessful());
        assertNotNull(r.getBody());
        assertTrue(r.getBody().contains("DeSubtitle"));
    }

    @Test
    void pathWithoutExtensionFallsBackToIndex() {
        ResponseEntity<String> r = restTemplate.getForEntity(baseUrl() + "/any/spa/route", String.class);
        assertTrue(r.getStatusCode().is2xxSuccessful());
        assertNotNull(r.getBody());
        assertTrue(r.getBody().contains("<html"));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
