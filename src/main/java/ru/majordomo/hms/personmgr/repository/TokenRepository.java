package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.model.token.Token;

public interface TokenRepository extends MongoRepository<Token, String> {
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<Token> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<Token> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    Token findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
    Token findByIdAndDeletedIsNull(@Param("id") String id);
    Token findByIdAndTypeAndDeletedIsNull(@Param("id") String id, @Param("type") TokenType tokenType);
    Token findByTypeAndDeletedIsNullAndPersonalAccountId(@Param("type") TokenType tokenType, @Param("personalAccountId") String personalAccountId);
    Stream<Token> findByCreatedBeforeOrderByCreatedDateAsc(@Param("created") LocalDateTime createdDate);
    void deleteByTypeAndCreatedBefore(@Param("type") TokenType type, @Param("created") LocalDateTime created);
}