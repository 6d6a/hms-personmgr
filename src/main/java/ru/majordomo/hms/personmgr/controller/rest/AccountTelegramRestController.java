package ru.majordomo.hms.personmgr.controller.rest;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.TokenManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.telegram.AccountTelegram;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.AccountTelegramRepository;
import ru.majordomo.hms.personmgr.service.TelegramRestClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@AllArgsConstructor
public class AccountTelegramRestController extends CommonRestController {

    private final AccountTelegramRepository repository;
    private final TokenManager tokenManager;
    private final TelegramRestClient telegramRestClient;

    @RequestMapping(value = "/{accountId}/telegram/token", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getToken(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        try {
            PersonalAccount account = accountManager.findOne(accountId);
            String botName;
            try {
                botName = telegramRestClient.callGetTelegramBotName();
            } catch (FeignException | BaseException ex) {
                botName = "";
                log.error("We got exception when ask telegram bot name", ex);
            }

            Token token = tokenManager.getToken(TokenType.TELEGRAM, account.getId());
            String responseToken = token == null ?
                    tokenManager.generateToken(account, TokenType.TELEGRAM, null) : token.getId();

            Map<String, String> response = new HashMap<>();
            response.put("token", responseToken);
            response.put("botName", botName);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("We got unrecognized exception when create telegram token", ex);
            ex.printStackTrace();
            throw new InternalApiException();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/telegram/confirm", method = RequestMethod.POST)
    public ResponseEntity<Void> save(
            @RequestBody Map<String, String> requestBody
    ) {

        String incomeToken = requestBody.get("token");

        if (incomeToken == null) {
            throw new ParameterValidationException("Токен некорректный");
        }

        Token token = tokenManager.getToken(incomeToken, TokenType.TELEGRAM);

        if (token == null) {
            throw new ParameterValidationException("Токен не найден");
        }

        String chatId = requestBody.get("chatid");
        String data = requestBody.get("data");

        if (!token.getId().equals(incomeToken)) {
            throw new ParameterValidationException("Токен не совпадает");
        }

        if (chatId == null || chatId.equals("")) {
            throw new ParameterValidationException("ID чата некорректный");
        }

        AccountTelegram accountTelegram = new AccountTelegram();
        accountTelegram.setActive(true);
        accountTelegram.setChatId(chatId);
        accountTelegram.setData(data);
        accountTelegram.setCreated(LocalDateTime.now());
        accountTelegram.setPersonalAccountId(token.getPersonalAccountId());
        repository.save(accountTelegram);

        tokenManager.deleteToken(token);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/telegram", method = RequestMethod.GET)
    public ResponseEntity<AccountTelegram> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Optional<AccountTelegram> accountTelegram = repository.findByPersonalAccountId(account.getId());
        return accountTelegram.map(telegram -> new ResponseEntity<>(telegram, HttpStatus.OK)).orElseGet(
                () -> new ResponseEntity<>(HttpStatus.NO_CONTENT)
        );
    }

    @RequestMapping(value = "/{accountId}/telegram", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Optional<AccountTelegram> accountTelegram = repository.findByPersonalAccountId(account.getId());
        accountTelegram.ifPresent(repository::delete);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}