package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.kitepromiss.desubtitle.testsupport.AlwaysInitializedAccessGateConfig;
import com.kitepromiss.desubtitle.testsupport.VideoUploadMaxOneConfig;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({AlwaysInitializedAccessGateConfig.Beans.class, VideoUploadMaxOneConfig.class})
@ActiveProfiles("video-upload-quota-one")
class VideoUploadControllerQuotaTest {

    @LocalServerPort
    private int port;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
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

    @Test
    void secondUploadForSameUserReturns409() throws Exception {
        String jwt = fetchToken();
        assertEquals(HttpStatus.OK, upload(jwt, "a.mp4", new byte[] {1}).getStatusCode());
        ResponseEntity<String> second = upload(jwt, "b.mp4", new byte[] {2});
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
        assertTrue(second.getBody().contains("video_quota_exceeded"));
    }

    private ResponseEntity<String> upload(String jwt, String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        ByteArrayResource file =
                new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        return restTemplate.exchange(
                baseUrl() + "/uploadVideo", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
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
