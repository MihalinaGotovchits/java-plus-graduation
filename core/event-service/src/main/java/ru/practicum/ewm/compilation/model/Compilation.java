package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.event.model.Event;

import java.util.List;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Boolean pinned;

    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events;
}
