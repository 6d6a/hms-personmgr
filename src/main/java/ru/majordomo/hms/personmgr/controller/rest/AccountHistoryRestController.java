package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.QAccountHistory;
import ru.majordomo.hms.personmgr.querydsl.AccountHistoryQuerydslBinderCustomizer;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@Validated
public class AccountHistoryRestController extends CommonRestController {

    @PreAuthorize("hasAuthority('ACCOUNT_HISTORY_VIEW')")
    @RequestMapping(value = "/{accountId}/account-history/{accountHistoryId}", method = RequestMethod.GET)
    public ResponseEntity<AccountHistory> getAccountHistory(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountHistory.class) @PathVariable(value = "accountHistoryId") String accountHistoryId
    ) {
        AccountHistory accountHistory = history.findByIdAndPersonalAccountId(accountHistoryId, accountId);

        if(accountHistory == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(accountHistory, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_HISTORY_VIEW')")
    @RequestMapping(value = "/{accountId}/account-history", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountHistory>> getAccountHistoryAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @QuerydslPredicate(
                    root = AccountHistory.class,
                    bindings = AccountHistoryQuerydslBinderCustomizer.class
            ) Predicate predicate,
            Pageable pageable
    ) {
        QAccountHistory qAccountHistory = QAccountHistory.accountHistory;

        BooleanBuilder builder = new BooleanBuilder();
        Predicate personalAccountIdPredicate = builder.and(qAccountHistory.personalAccountId.eq(accountId));

        if(predicate == null) {
            predicate = personalAccountIdPredicate;
        } else {
            predicate = ExpressionUtils.allOf(predicate, personalAccountIdPredicate);
        }

        Page<AccountHistory> accountHistories = history.findAll(predicate, pageable);

        return new ResponseEntity<>(accountHistories, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_HISTORY_VIEW')")
    @RequestMapping(value = "/account-history", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountHistory>> getHistoryAll(
            @QuerydslPredicate(
                    root = AccountHistory.class,
                    bindings = AccountHistoryQuerydslBinderCustomizer.class
            ) Predicate predicate,
            Pageable pageable
    ) {
        Page<AccountHistory> accountHistories = history.findAll(predicate, pageable);

        return new ResponseEntity<>(accountHistories, HttpStatus.OK);
    }

    //TODO надо переделать контроллер для того чтобы нельзя было добавлять сообщения от имени другого пользователя
    // сейчас через биллинг можно передать какое угодно имя оператора
    @PreAuthorize("hasAuthority('ACCOUNT_HISTORY_ADD')")
    @RequestMapping(value = "/{accountId}/account-history", method = RequestMethod.POST)
    public ResponseEntity<AccountHistory> addAccountHistory(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String historyMessage = requestBody.get(HISTORY_MESSAGE_KEY);
        String operator = requestBody.get(OPERATOR_KEY);
        operator = (operator == null || operator.equals("")) ? request.getUserPrincipal().getName() : operator;

        if (historyMessage != null && operator != null) {
            history.addMessage(accountId, historyMessage, operator);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}