package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@Validated
public class AccountControlNotificationRestController extends CommonRestController {

    private final AccountNoticeRepository accountNoticeRepository;

    @Autowired
    public AccountControlNotificationRestController(
            AccountNoticeRepository accountNoticeRepository
    ) {
        this.accountNoticeRepository = accountNoticeRepository;
    }

    //Список всех нотификций
    @GetMapping("/{accountId}/account-notices")
    public ResponseEntity<List<AccountNotice>> listAllNotices(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountNotice> notices = accountNoticeRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    //Список всех не просмотренных нотификций
    @GetMapping("/{accountId}/account-notices/new")
    public ResponseEntity<List<AccountNotice>> listNotViewedNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountNotice> notices = accountNoticeRepository.findByPersonalAccountIdAndViewed(accountId, false);

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    //Пометить прочитанным
    @PostMapping("/{accountId}/account-notices/{accountNoticeId}/mark")
    public ResponseEntity<Void> confirm(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountControlNotificationId") String accountControlNotificationId
    ) {
        AccountNotice notification = accountNoticeRepository.findByPersonalAccountIdAndId(accountId, accountControlNotificationId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountControlNotificationId + "' не найдено");
        }

        notification.setViewed(true);
        accountNoticeRepository.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}