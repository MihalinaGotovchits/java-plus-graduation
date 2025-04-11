package ru.practicum.client;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.StatResponseDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class StatClient extends BaseClient {

    private final String serverUrl;

    public StatClient(@Value("${baseurl-statservice:http://localhost:9092}") String serverUrl) {
        super(new RestTemplate());
        this.serverUrl = serverUrl;
    }

    public ResponseEntity<Object> addStatEvent(StatDto statDto) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/hit")
                .build()
                .toUriString();
        return post(url, statDto);
    }

    public List<StatResponseDto> readStatEvent(String start, String end,
                                               @Nullable List<String> uris, boolean unique) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/stats")
                .queryParam("unique", unique);

        if (start != null) {
            uriBuilder.queryParam("start", encode(start));
        }
        if (end != null) {
            uriBuilder.queryParam("end", encode(end));
        }
        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris", String.join(",", uris));
        }

        return get(uriBuilder.build().toUriString(), null);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}