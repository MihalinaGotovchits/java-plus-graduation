package ru.practicum.dto;

import lombok.*;

@Getter
@Setter
public class StatResponseDto {
    private String app;
    private String uri;
    private Long hits;

    public StatResponseDto(String app, String uri, Long hits) {
        this.app = app;
        this.uri = uri;
        this.hits = hits;
    }
}
