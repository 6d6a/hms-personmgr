package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.token.Token;

import java.time.LocalDateTime;
import java.util.Map;

public interface TokenManager {
    String generateToken(PersonalAccount account, TokenType type, Map<String, Object> params);

    String generateToken(PersonalAccount account, TokenType type);

    Token findOne(String id);

    Token getToken(TokenType tokenType, String personalAccountId);

    Token getToken(String id, TokenType tokenType);

    void deleteToken(Token token);

    void deleteByTypeAndCreatedBefore(TokenType type, LocalDateTime created);

    void setDeletedByCreatedBefore(LocalDateTime created);
}
