package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.model.token.Token;

public interface TokenRepository extends MongoRepository<Token, String> {
    List<Token> findByPersonalAccountId(String personalAccountId);
    Page<Token> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    Token findByIdAndPersonalAccountId(String id, String personalAccountId);
    Token findByIdAndTypeAndDeletedIsNull(String id, TokenType tokenType);
    Token findByTypeAndDeletedIsNullAndPersonalAccountId(TokenType tokenType, String personalAccountId);
    void deleteByTypeAndCreatedBefore(TokenType type, LocalDateTime created);
}