package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsRequestParamsDto;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@ComponentScan(value = {"ru.practicum.ewm", "ru.practicum.stats.client"})
public class MainApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MainApplication.class, args);
        StatClient statClient = context.getBean(StatClient.class);

        LocalDateTime startDateTime = LocalDateTime.now();

        HitDto testHitDto = HitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .build();

        statClient.hit(testHitDto);
        statClient.hit(testHitDto);
        statClient.hit(testHitDto.toBuilder().uri("/events").timestamp(LocalDateTime.now()).build());

        // get stats
        StatsRequestParamsDto statsRequestParamsDto = StatsRequestParamsDto.builder()
                .start(startDateTime)
                .end(LocalDateTime.now())
                .build();

        System.out.println("Not unique: " + statClient.get(statsRequestParamsDto));
        System.out.println("Unique: " + statClient.get(statsRequestParamsDto.toBuilder().unique(true).build()));
        System.out.println("By uris: " + statClient.get(statsRequestParamsDto.toBuilder()
                .unique(true)
                .uris(List.of("/events"))
                .build())
        );
        System.out.println("Not valid: " + statClient.get(statsRequestParamsDto.toBuilder().start(LocalDateTime.now()).build()));
    }
}