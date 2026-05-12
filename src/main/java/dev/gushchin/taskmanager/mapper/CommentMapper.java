package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.CommentsRecord;
import dev.gushchin.taskmanager.model.Comment;

public final class CommentMapper {

    private CommentMapper() {}

    public static Comment toModel(CommentsRecord record) {
        if (record == null) {
            return null;
        }

        return new Comment(
                record.getId(),
                record.getTaskId(),
                record.getUserId(),
                record.getMessage(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
