package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.Token;
import ru.majordomo.hms.personmgr.repository.TokenRepository;

@Service
public class TokenHelper {
    private final TokenRepository repository;

    @Autowired
    public TokenHelper(TokenRepository repository) {
        this.repository = repository;
    }

    public String generateToken(PersonalAccount account, TokenType type) {
        Token token = new Token();
        token.setPersonalAccountId(account.getId());
        token.setType(type);

        repository.insert(token);

        return token.getId();
    }

    public Token getToken(String id) {
        return repository.findByIdAndDeletedIsNull(id);
    }

    public void deleteToken(Token token) {
        token.setDeleted(LocalDateTime.now());

        repository.save(token);
    }
}
