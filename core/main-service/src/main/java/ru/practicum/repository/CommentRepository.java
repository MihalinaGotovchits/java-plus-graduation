package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.comment.CountCommentsByEventDto;
import ru.practicum.model.Comment;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByEventId(Long eventId, Pageable pageable);

    List<Comment> findAllByAuthorId(Long userId);

    Optional<Comment> findByAuthorIdAndId(Long userId, Long id);

    @Query("select new ru.practicum.dto.comment.CountCommentsByEventDto(c.event.id, COUNT(c)) " +
            "from comments c where c.event.id in ?1 " +
            "GROUP BY c.event.id")
    List<CountCommentsByEventDto> countCommentByEvent(List<Long> eventIds);

    @Query("select c " +
            "from comments as c " +
            "where lower(c.text) like lower(concat('%', ?1, '%') )")
    List<Comment> search(String text, Pageable pageable);
}
