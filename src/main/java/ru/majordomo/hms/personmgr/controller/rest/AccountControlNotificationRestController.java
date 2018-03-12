package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountControlNotification;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountControlNotificationRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@Validated
public class AccountControlNotificationRestController extends CommonRestController {

    private final AccountControlNotificationRepository accountControlNotificationRepository;

    @Autowired
    public AccountControlNotificationRestController(
            AccountControlNotificationRepository accountControlNotificationRepository
    ) {
        this.accountControlNotificationRepository = accountControlNotificationRepository;
    }

    //Список всех нотификций
    @GetMapping("/{accountId}/account-notification")
    public ResponseEntity<List<AccountControlNotification>> listAllNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountControlNotification> notifications = accountControlNotificationRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    //Список всех не просмотренных нотификций
    @GetMapping("/{accountId}/account-notification/new")
    public ResponseEntity<List<AccountControlNotification>> listNotViewedNotifications(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountControlNotification> notifications = accountControlNotificationRepository.findByPersonalAccountIdAndViewed(accountId, false);

        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @PostMapping("/{accountId}/account-notification/confirm/{accountControlNotificationId}")
    public ResponseEntity<Void> confirm(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountControlNotificationId") String accountControlNotificationId
    ) {
        AccountControlNotification notification = accountControlNotificationRepository.findByPersonalAccountIdAndId(accountId, accountControlNotificationId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountControlNotificationId + "' не найдено");
        }

        notification.setViewed(true);
        accountControlNotificationRepository.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}