package ru.practicum.controller.privates;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/comments")
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping("/users/{userId}/events/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @RequestBody @Valid NewCommentDto commentDto) {
        log.info("POST запрос на добавление комментария: {}", commentDto);
        return commentService.createComment(userId, eventId, commentDto);
    }

    @PatchMapping("/users/{userId}/{commentId}")
    public CommentDto patchCommentByUser(@PathVariable Long userId,
                                         @PathVariable Long commentId,
                                         @RequestBody @Valid UpdateCommentDto updateCommentDto) {
        log.info("PATCH запрос на обновление пользователем с Id {} комментария с Id {}", userId, commentId);
        return commentService.patchByUser(userId, commentId, updateCommentDto);
    }

    @GetMapping("/users/{userId}/comments")
    public List<CommentDto> getCommentsListUser(@PathVariable Long userId) {
        log.info("GET запрос на получение списка комментариев пользователя с Id {}", userId);
        return commentService.getCommentUser(userId);
    }

    @DeleteMapping("/users/{userId}/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("DELETE запрос на удаление комментария с Id {} пользователем с Id {}", commentId, userId);
        commentService.deleteComment(userId, commentId);
    }

    @GetMapping("/users/{userId}/{commentId}")
    public Comment getComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("GET запрос на получение комментария с Id {} пользователем с Id {}", commentId, userId);
        return commentService.getUserCommentByUserAndCommentId(userId, commentId);
    }
}
