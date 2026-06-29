package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.repository.CommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final UserService userService;

    public Comment findById(Long id) {
        Comment comment = commentRepository.findById(id);

        if (comment == null || comment.isDeleted()) {
            throw new IllegalArgumentException("Comment not found " + id);
        }

        return comment;
    }

    public List<Comment> findByTaskId(Long taskId) {
        taskService.findById(taskId);

        return commentRepository.findByTaskId(taskId).stream()
                .filter(Predicate.not(Comment::isDeleted))
                .toList();
    }

    public Comment create(Long taskId, UUID userId, String message) {
        taskService.findById(taskId);
        userService.findById(userId);

        Instant now = Instant.now();

        Comment comment = new Comment(null, taskId, userId, message, now, now, false);

        return commentRepository.save(comment);
    }

    public Comment updateMessage(Long id, String message, UUID userId) {
        Comment comment = findById(id);

        checkCanUpdateComment(comment, userId);

        return commentRepository.updateMessage(comment.getId(), message, Instant.now());
    }

    public Comment deleteById(Long id, UUID userId) {
        Comment comment = findById(id);

        checkCanUpdateComment(comment, userId);

        return commentRepository.markAsDeleted(comment.getId(), Instant.now());
    }

    private void checkCanUpdateComment(Comment comment, UUID userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedForTaskException();
        }
    }
}
