package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatDto {
    @NotBlank(groups = Create.class)
    @Size(max = 50, groups = Create.class)
    private String app;

    @NotBlank(groups = Create.class)
    @Size(max = 50, groups = Create.class)
    private String uri;

    private Set<String> uris;

    @NotBlank(groups = Create.class)
    @Size(max = 15, groups = Create.class)
    private String ip;

    @NotNull(groups = Create.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime timestamp;
}
