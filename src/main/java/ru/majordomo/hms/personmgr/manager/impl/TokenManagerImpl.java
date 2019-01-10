package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.manager.TokenManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.TokenRepository;

@Component
public class TokenManagerImpl implements TokenManager {
    private final TokenRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public TokenManagerImpl(TokenRepository repository, MongoOperations mongoOperations) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public String generateToken(PersonalAccount account, TokenType type, Map<String, Object> params) {
        Token token = new Token();
        token.setPersonalAccountId(account.getId());
        token.setType(type);
        if (params != null) { token.setParams(params); }

        repository.insert(token);

        return token.getId();
    }

    @Override
    public String generateToken(PersonalAccount account, TokenType type) {
        return generateToken(account, type, null);
    }

    @Override
    public Token findOne(String id) {
        return repository.findOne(id);
    }

    @Override
    public Token getToken(TokenType tokenType, String personalAccountId) { return repository.findByTypeAndDeletedIsNullAndPersonalAccountId(tokenType, personalAccountId); }

    @Override
    public Token getToken(String id, TokenType tokenType) {
        return repository.findByIdAndTypeAndDeletedIsNull(id, tokenType);
    }

    @Override
    public void deleteToken(Token token) {
        token.setDeleted(LocalDateTime.now());

        repository.save(token);
    }

    @Override
    public void deleteByTypeAndCreatedBefore(TokenType type, LocalDateTime created) {
        repository.deleteByTypeAndCreatedBefore(type, created);
    }

    @Override
    public void setDeletedByCreatedBefore(LocalDateTime created) {
        mongoOperations.updateMulti(
                new Query(
                        new Criteria("deleted").exists(false)
                                .and("created").lte(Date.from(created.toInstant(ZoneOffset.ofHours(3))))
                ),
                new Update().currentDate("deleted"),
                Token.class
        );
    }
}
