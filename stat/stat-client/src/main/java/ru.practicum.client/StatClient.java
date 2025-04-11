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

    @Value("${baseurl-statservice}")
    private String serverUrl;

    public StatClient() {
        super(createRestTemplate());
    }

    private static RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    public ResponseEntity<Object> addStatEvent(StatDto statDto) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(serverUrl).path("/hit");
        String url = uri.build().toUriString();
        return post(url, statDto);
    }

    public List<StatResponseDto> readStatEvent(String start, String end, @Nullable List<String> uris, boolean unique) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(serverUrl).path("/stats");

        if (start != null) {
            uri.queryParam("start", encode(start));
        }
        if (end != null) {
            uri.queryParam("end", encode(end));
        }
        if (uris != null) {
            uri.queryParam("uris", uris);
        }
        String url = uri.build().toUriString();
        return get(url, null);
    }

    private String encode(String value) {
        if (value != null) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } else {
            return value;
        }
    }
}