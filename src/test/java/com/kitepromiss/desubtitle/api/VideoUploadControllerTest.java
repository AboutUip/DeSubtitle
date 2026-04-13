package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.kitepromiss.desubtitle.testsupport.AlwaysInitializedAccessGateConfig;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AlwaysInitializedAccessGateConfig.Beans.class)
class VideoUploadControllerTest {

    @LocalServerPort
    private int port;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void uploadVideoWithBearerReturnsMetadata() throws Exception {
        String jwt = fetchToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);

        ByteArrayResource file =
                new ByteArrayResource("fake-video".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "clip.mp4";
                    }
                };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        ResponseEntity<String> res =
                restTemplate.exchange(
                        baseUrl() + "/uploadVideo",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        JsonNode n = jsonMapper.readTree(res.getBody());
        assertTrue(n.path("id").isTextual());
        assertTrue(n.path("storedFileName").asText().endsWith(".mp4"));
        assertEquals("clip.mp4", n.path("originalFileName").asText());
    }

    private String fetchToken() throws Exception {
        ResponseEntity<String> tokenRes = restTemplate.getForEntity(baseUrl() + "/getUserToken", String.class);
        assertTrue(tokenRes.getStatusCode().is2xxSuccessful(), tokenRes.getBody());
        return jsonMapper.readTree(tokenRes.getBody()).path("token").asText();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
