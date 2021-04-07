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
import ru.majordomo.hms.personmgr.model.account.BirthdayAccountNotice;
import ru.majordomo.hms.personmgr.model.account.DefaultAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.Constants.ACTION_BIRTHDAY_21_END_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.ACTION_BIRTHDAY_21_START_DATE;

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

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(ACTION_BIRTHDAY_21_START_DATE, formatter);
        LocalDateTime endDate = LocalDateTime.parse(ACTION_BIRTHDAY_21_END_DATE, formatter);
        if (now.isAfter(startDate) && now.isBefore(endDate)) {

            List<BirthdayAccountNotice> birthdayNotices = accountNoticeManager.findBirthdayAccountNoticeByPersonalAccountId(accountId);

            if (birthdayNotices.isEmpty()) {
                BirthdayAccountNotice notice = new BirthdayAccountNotice();
                notice.setPersonalAccountId(accountId);
                notice.setViewed(false);

                accountNoticeManager.insert(notice);
            } else {
                if (birthdayNotices.stream().allMatch(AccountNotice::isViewed)) {
                    birthdayNotices.stream().filter(AccountNotice::isViewed).findFirst().ifPresent(item -> {
                        if (item.getViewedDate() != null && item.getViewedDate().isBefore(LocalDateTime.now().minusHours(72))) {
                            //После закрытия 72 часа не показывается. Потом показывается снова. И так до момента завершения акции.
                            item.setViewed(false);
                            accountNoticeManager.save(item);
                        }
                    });
                }
            }
        }

        if (now.isAfter(endDate)) {
            List<BirthdayAccountNotice> birthdayNotices = accountNoticeManager.findBirthdayAccountNoticeByPersonalAccountId(accountId);
            birthdayNotices.forEach(item -> {
                item.setViewed(true);
                item.setViewedDate(LocalDateTime.now());
                accountNoticeManager.save(item);
            });
        }

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
        Optional<BirthdayAccountNotice> n = accountNoticeManager.findBirthdayAccountNoticeByPersonalAccountIdAndId(
                accountId, accountNoticeId
        );

        if (n.isPresent()) {
            n.get().setViewed(true);
            n.get().setViewedDate(LocalDateTime.now());
            accountNoticeManager.save(n.get());

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

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