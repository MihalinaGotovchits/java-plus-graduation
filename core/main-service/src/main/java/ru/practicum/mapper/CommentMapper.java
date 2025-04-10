package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthor().getId())
                .created(comment.getCreated())
                .lastUpdateOn(comment.getLastUpdateOn())
                .build();
    }

    public Comment toComment(NewCommentDto newCommentDto, Event event, User user) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .author(user)
                .created(LocalDateTime.now())
                .lastUpdateOn(null)
                .build();
    }

    public Comment toComment(CommentDto commentDto) {
        return Comment.builder()
                .text(commentDto.getText())
                .build();
    }
}
