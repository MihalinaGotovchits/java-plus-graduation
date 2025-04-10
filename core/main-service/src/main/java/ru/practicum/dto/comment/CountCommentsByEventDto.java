package ru.practicum.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountCommentsByEventDto {

    private Long eventId;

    private Long countComments;
}
