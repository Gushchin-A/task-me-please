package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;

import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.mapper.AccountTokenMapper;
import dev.gushchin.taskmanager.model.AccountToken;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountTokenRepository {
    private final DSLContext dsl;

    public AccountToken findByTokenHash(String tokenHash) {
        AccountTokensRecord record = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.TOKEN_HASH.eq(tokenHash))
                .fetchOne();

        return AccountTokenMapper.toModel(record);
    }

    public AccountToken save(AccountToken accountToken) {
        AccountTokensRecord record = dsl.insertInto(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.USER_ID, accountToken.getUserId())
                .set(ACCOUNT_TOKENS.TYPE, accountToken.getType().name())
                .set(ACCOUNT_TOKENS.TOKEN_HASH, accountToken.getTokenHash())
                .set(ACCOUNT_TOKENS.EXPIRES_AT, accountToken.getExpiresAt().atOffset(ZoneOffset.UTC))
                .set(
                        ACCOUNT_TOKENS.USED_AT,
                        accountToken.getUsedAt() == null
                                ? null
                                : accountToken.getUsedAt().atOffset(ZoneOffset.UTC))
                .set(ACCOUNT_TOKENS.CREATED_AT, accountToken.getCreatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return AccountTokenMapper.toModel(record);
    }

    public void deleteAll() {
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
    }
}
