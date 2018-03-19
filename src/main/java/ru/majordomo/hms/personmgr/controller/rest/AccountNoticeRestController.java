package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@Validated
public class AccountNoticeRestController extends CommonRestController {

    private final AccountNoticeRepository accountNoticeRepository;

    @Autowired
    public AccountNoticeRestController(
            AccountNoticeRepository accountNoticeRepository
    ) {
        this.accountNoticeRepository = accountNoticeRepository;
    }

    //Список всех нотификций
    @GetMapping("/{accountId}/account-notices")
    public ResponseEntity<List<AccountNotice>> listAllNotices(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "type", required = false) AccountNoticeType type
    ) {
        List<AccountNotice> notices;

        if (type == null) {
            notices = accountNoticeRepository.findByPersonalAccountId(accountId);
        } else {
           notices = accountNoticeRepository.findByPersonalAccountIdAndType(accountId, type);
        }

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-notices/{accountNoticeId}")
    public ResponseEntity<AccountNotice> getOne(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountNotice.class) @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notice = accountNoticeRepository.findByPersonalAccountIdAndId(accountId, accountNoticeId);

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
            notices = accountNoticeRepository.findByPersonalAccountIdAndViewed(accountId, false);
        } else {
            notices = accountNoticeRepository.findByPersonalAccountIdAndViewedAndType(accountId, false, type);
        }

        return new ResponseEntity<>(notices, HttpStatus.OK);
    }

    //Пометить прочитанным
    @PostMapping("/{accountId}/account-notices/{accountNoticeId}/mark")
    public ResponseEntity<Void> mark(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notification = accountNoticeRepository.findByPersonalAccountIdAndId(accountId, accountNoticeId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountNoticeId + "' не найдено");
        }

        notification.setViewed(true);
        accountNoticeRepository.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    //Пометить НЕ прочитанным
    @PostMapping("/{accountId}/account-notices/{accountNoticeId}/unmark")
    public ResponseEntity<Void> unmark(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountNoticeId") String accountNoticeId
    ) {
        AccountNotice notification = accountNoticeRepository.findByPersonalAccountIdAndId(accountId, accountNoticeId);

        if (notification == null) {
            throw new ParameterValidationException("Уведомление с ID: '" + accountNoticeId + "' не найдено");
        }

        notification.setViewed(false);
        accountNoticeRepository.save(notification);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}