package ru.practicum.client;

import jakarta.annotation.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class BaseClient {
    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected <T> List<T> get(String path, @Nullable Map<String, Object> parameters) {
        ResponseEntity<List<T>> response = rest.exchange(
                path,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                parameters != null ? parameters : new Object[0]
        );
        return response.getBody();
    }

    protected <T> ResponseEntity<Object> post(String path, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, null, body);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(
            HttpMethod method, String path,
            @Nullable Map<String, Object> parameters,
            @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        try {
            return parameters != null ?
                    rest.exchange(path, method, requestEntity, Object.class, parameters) :
                    rest.exchange(path, method, requestEntity, Object.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}