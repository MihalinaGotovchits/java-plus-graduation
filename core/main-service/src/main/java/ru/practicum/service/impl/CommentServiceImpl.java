package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.UncorrectedParametersException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.CommentService;
import ru.practicum.status.event.State;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto commentDto) {
        User user = checkUser(userId);
        Event event = checkEvent(eventId);
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new UncorrectedParametersException("Невозможно добавить комментарий к неопубликованному событию");
        }
        return CommentMapper.toCommentDto(commentRepository.save(CommentMapper.toComment(commentDto, event, user)));
    }

    @Override
    @Transactional
    public CommentDto patchByUser(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        User user = checkUser(userId);
        Comment comment = checkComment(commentId);
        checkAuthorComment(user, comment);
        LocalDateTime updateTime = LocalDateTime.now();

        if (updateTime.isAfter(comment.getCreated().plusHours(1))) {
            throw new UncorrectedParametersException("Отредактировать комментарий возможно " +
                    "только в течение часа после его публикации");
        }
        comment.setText(updateCommentDto.getText());
        comment.setLastUpdateOn(updateTime);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentUser(Long userId) {
        checkUser(userId);
        List<Comment> commentList = commentRepository.findAllByAuthorId(userId);
        return commentList.stream().map(CommentMapper::toCommentDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Comment getUserCommentByUserAndCommentId(Long userId, Long commentId) {
        checkUser(userId);
        return commentRepository.findByAuthorIdAndId(userId, commentId).orElseThrow(
                () -> new NotFoundException("У пользователя с Id: " + userId + "не найден комментарий с Id: " + commentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentEvent(Long eventId, Integer from, Integer size) {
        Event event = checkEvent(eventId);
        PageRequest pageable = PageRequest.of(from / size, size);
        return commentRepository.findAllByEventId(eventId, pageable);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        User user = checkUser(userId);
        Comment comment = checkComment(commentId);
        checkAuthorComment(user, comment);
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = checkComment(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public List<Comment> search(String text, Integer from, Integer size) {
        PageRequest pageable = PageRequest.of(from / size, size);
        if (text.isBlank()) {
            return Collections.emptyList();
        }
        return commentRepository.search(text, pageable);
    }

    private User checkUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException(
                "Пользователь с Id: " + userId + "не найден"));
    }

    private Comment checkComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий с Id: " + commentId + "не найден"));
    }

    private void checkAuthorComment(User user, Comment comment) {
        if (!comment.getAuthor().equals(user)) {
            throw new UncorrectedParametersException("Пользователь не является автором комментария");
        }
    }

    private Event checkEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с Id: " + eventId + "не найдено"));
    }
}
