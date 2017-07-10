package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.TokenRepository;

@Service
public class TokenHelper {
    private final TokenRepository repository;

    @Autowired
    public TokenHelper(TokenRepository repository) {
        this.repository = repository;
    }

    public String generateToken(PersonalAccount account, TokenType type, Map<String, Object> params) {
        Token token = new Token();
        token.setPersonalAccountId(account.getId());
        token.setType(type);
        if (params != null) { token.setParams(params); }

        repository.insert(token);

        return token.getId();
    }

    public String generateToken(PersonalAccount account, TokenType type) {
        return generateToken(account, type, null);
    }

    public Token getToken(String id) {
        return repository.findByIdAndDeletedIsNull(id);
    }

    public Token getToken(String id, TokenType tokenType) {
        return repository.findByIdAndTypeAndDeletedIsNull(id, tokenType);
    }

    public void deleteToken(Token token) {
        token.setDeleted(LocalDateTime.now());

        repository.save(token);
    }
}
