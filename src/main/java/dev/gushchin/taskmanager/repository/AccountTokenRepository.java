package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;

import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.mapper.AccountTokenMapper;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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

    public List<AccountToken> findByUserIdAndType(UUID userId, AccountTokenType type) {
        return dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(userId))
                .and(ACCOUNT_TOKENS.TYPE.eq(type.name()))
                .orderBy(ACCOUNT_TOKENS.CREATED_AT.asc())
                .fetch()
                .map(AccountTokenMapper::toModel);
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
                .set(
                        ACCOUNT_TOKENS.INVALIDATED_AT,
                        accountToken.getInvalidatedAt() == null
                                ? null
                                : accountToken.getInvalidatedAt().atOffset(ZoneOffset.UTC))
                .set(ACCOUNT_TOKENS.CREATED_AT, accountToken.getCreatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return AccountTokenMapper.toModel(record);
    }

    public void invalidateActiveByUserIdAndType(UUID userId, AccountTokenType type, Instant usedAt) {
        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.USED_AT, usedAt.atOffset(ZoneOffset.UTC))
                .where(ACCOUNT_TOKENS.USER_ID.eq(userId))
                .and(ACCOUNT_TOKENS.TYPE.eq(type.name()))
                .and(ACCOUNT_TOKENS.USED_AT.isNull())
                .and(ACCOUNT_TOKENS.INVALIDATED_AT.isNull())
                .and(ACCOUNT_TOKENS.EXPIRES_AT.gt(usedAt.atOffset(ZoneOffset.UTC)))
                .execute();
    }

    public void invalidateReplacedByUserIdAndType(UUID userId, AccountTokenType type, Instant invalidatedAt) {
        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.INVALIDATED_AT, invalidatedAt.atOffset(ZoneOffset.UTC))
                .where(ACCOUNT_TOKENS.USER_ID.eq(userId))
                .and(ACCOUNT_TOKENS.TYPE.eq(type.name()))
                .and(ACCOUNT_TOKENS.USED_AT.isNull())
                .and(ACCOUNT_TOKENS.INVALIDATED_AT.isNull())
                .and(ACCOUNT_TOKENS.EXPIRES_AT.gt(invalidatedAt.atOffset(ZoneOffset.UTC)))
                .execute();
    }

    public AccountToken markUsedIfActive(Long id, Instant usedAt) {
        AccountTokensRecord record = dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.USED_AT, usedAt.atOffset(ZoneOffset.UTC))
                .where(ACCOUNT_TOKENS.ID.eq(id))
                .and(ACCOUNT_TOKENS.USED_AT.isNull())
                .and(ACCOUNT_TOKENS.INVALIDATED_AT.isNull())
                .and(ACCOUNT_TOKENS.EXPIRES_AT.gt(usedAt.atOffset(ZoneOffset.UTC)))
                .returning()
                .fetchOne();

        return AccountTokenMapper.toModel(record);
    }

    public void deleteByTokenHash(String tokenHash) {
        dsl.deleteFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.TOKEN_HASH.eq(tokenHash))
                .execute();
    }

    public void deleteAll() {
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
    }
}
