package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;

public final class AccountTokenMapper {

    private AccountTokenMapper() {}

    public static AccountToken toModel(AccountTokensRecord record) {
        if (record == null) {
            return null;
        }

        return new AccountToken(
                record.getId(),
                record.getUserId(),
                AccountTokenType.valueOf(record.getType()),
                record.getTokenHash(),
                record.getExpiresAt().toInstant(),
                record.getUsedAt() == null ? null : record.getUsedAt().toInstant(),
                record.getCreatedAt().toInstant());
    }
}
