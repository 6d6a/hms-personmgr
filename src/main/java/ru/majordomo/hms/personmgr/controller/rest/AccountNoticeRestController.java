package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountNoticeManager;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@Validated
public class AccountNoticeRestController extends CommonRestController {

    private final AccountNoticeManager accountNoticeManager;

    @Autowired
    public AccountNoticeRestController(
            AccountNoticeManager accountNoticeManager
    ) {
        this.accountNoticeManager = accountNoticeManager;
    }

    //Список всех нотификций
    @GetMapping("/{accountId}/account-notices")
    public ResponseEntity<List<AccountNotice>> listAllNotices(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "type", required = false) AccountNoticeType type
    ) {
        List<AccountNotice> notices;

        if (type == null) {
            notices = accountNoticeManager.findByPersonalAccountId(accountId);
        } else {
           notices = accountNoticeManager.findByPersonalAccountIdAndType(accountId, type);
        }

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-notices/{accountNoticeId}")
    public ResponseEntity<AccountNotice> getOne(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountNotice.class) @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notice = accountNoticeManager.findByPersonalAccountIdAndId(accountId, accountNoticeId);

        return new ResponseEntity<>(notice, HttpStatus.OK);
    }

    //Список всех не просмотренных нотификций
    @GetMapping("/{accountId}/account-notices/new")
    public ResponseEntity<List<AccountNotice>> listNotViewedNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "type", required = false) AccountNoticeType type
    ) {
        List<AccountNotice> notices;

        if (type == null) {
            notices = accountNoticeManager.findByPersonalAccountIdAndViewed(accountId, false);
        } else {
            notices = accountNoticeManager.findByPersonalAccountIdAndViewedAndType(accountId, false, type);
        }

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    //Пометить прочитанным
    @PostMapping("/{accountId}/account-notices/{accountNoticeId}/mark")
    public ResponseEntity<Void> mark(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notification = accountNoticeManager.findByPersonalAccountIdAndId(accountId, accountNoticeId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountNoticeId + "' не найдено");
        }

        notification.setViewed(true);
        accountNoticeManager.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    //Пометить НЕ прочитанным
    @PostMapping("/{accountId}/account-notices/{accountNoticeId}/unmark")
    public ResponseEntity<Void> unmark(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notification = accountNoticeManager.findByPersonalAccountIdAndId(accountId, accountNoticeId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountNoticeId + "' не найдено");
        }

        notification.setViewed(false);
        accountNoticeManager.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}