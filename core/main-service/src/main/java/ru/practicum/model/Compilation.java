package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "compilations")
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(name = "compilations_events",
    joinColumns = @JoinColumn(name = "compilation_id"),
    inverseJoinColumns = @JoinColumn(name = "event_id"))
    private Set<Event> events;

    private Boolean pinned;

    private String title;
}
