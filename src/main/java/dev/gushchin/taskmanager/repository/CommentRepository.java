package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;

import dev.gushchin.taskmanager.jooq.tables.records.CommentsRecord;
import dev.gushchin.taskmanager.mapper.CommentMapper;
import dev.gushchin.taskmanager.model.Comment;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepository {
    private final DSLContext dsl;

    public Comment findById(Long id) {
        CommentsRecord record =
                dsl.selectFrom(COMMENTS).where(COMMENTS.ID.eq(id)).fetchOne();

        return CommentMapper.toModel(record);
    }

    public List<Comment> findByTaskId(Long taskId) {
        return dsl.selectFrom(COMMENTS)
                .where(COMMENTS.TASK_ID.eq(taskId))
                .orderBy(COMMENTS.CREATED_AT.asc())
                .fetch()
                .map(CommentMapper::toModel);
    }

    public Comment save(Comment comment) {
        CommentsRecord record = dsl.insertInto(COMMENTS)
                .set(COMMENTS.TASK_ID, comment.getTaskId())
                .set(COMMENTS.USER_ID, comment.getUserId())
                .set(COMMENTS.MESSAGE, comment.getMessage())
                .set(COMMENTS.IS_DELETED, comment.isDeleted())
                .set(COMMENTS.CREATED_AT, comment.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(COMMENTS.UPDATED_AT, comment.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return CommentMapper.toModel(record);
    }

    public Comment updateMessage(Long id, String message, Instant updatedAt) {
        CommentsRecord record = dsl.update(COMMENTS)
                .set(COMMENTS.MESSAGE, message)
                .set(COMMENTS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(COMMENTS.ID.eq(id))
                .returning()
                .fetchOne();

        return CommentMapper.toModel(record);
    }

    public Comment markAsDeleted(Long id, Instant updatedAt) {
        CommentsRecord record = dsl.update(COMMENTS)
                .set(COMMENTS.IS_DELETED, true)
                .set(COMMENTS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(COMMENTS.ID.eq(id))
                .returning()
                .fetchOne();

        return CommentMapper.toModel(record);
    }
}
