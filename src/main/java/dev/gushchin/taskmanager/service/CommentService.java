package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.repository.CommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final String BLANK_MESSAGE_ERROR = "Comment message must not be blank";

    private final CommentRepository commentRepository;
    private final NotificationPublisher notificationPublisher;
    private final TaskEmailService taskEmailService;
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

    @Transactional
    public Comment create(Long taskId, UUID userId, String message) {
        Task task = taskService.findByIdForUser(taskId, userId);
        userService.findById(userId);
        String preparedMessage = prepareMessage(message);

        Instant now = Instant.now();

        Comment comment = new Comment(null, taskId, userId, preparedMessage, now, now, false);

        Comment savedComment = commentRepository.save(comment);
        notificationPublisher.commentCreated(savedComment, task, userId);
        taskEmailService.sendCommentCreated(task, userId, savedComment.getId());

        return savedComment;
    }

    @Transactional
    public Comment updateMessage(Long id, String message, UUID userId) {
        Comment comment = findById(id);

        checkCanUpdateComment(comment, userId);
        String preparedMessage = prepareMessage(message);

        Comment updatedComment = commentRepository.updateMessage(comment.getId(), preparedMessage, Instant.now());
        notificationPublisher.commentUpdated(updatedComment, taskService.findById(comment.getTaskId()), userId);

        return updatedComment;
    }

    @Transactional
    public Comment deleteById(Long id, UUID userId) {
        Comment comment = findById(id);

        checkCanUpdateComment(comment, userId);

        Comment deletedComment = commentRepository.markAsDeleted(comment.getId(), Instant.now());
        notificationPublisher.commentDeleted(deletedComment, taskService.findById(comment.getTaskId()), userId);

        return deletedComment;
    }

    private void checkCanUpdateComment(Comment comment, UUID userId) {
        taskService.findByIdForUser(comment.getTaskId(), userId);

        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedForTaskException();
        }
    }

    private String prepareMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException(BLANK_MESSAGE_ERROR);
        }

        String preparedMessage = message.stripTrailing();

        if (preparedMessage.isBlank()) {
            throw new IllegalArgumentException(BLANK_MESSAGE_ERROR);
        }

        return preparedMessage;
    }
}
