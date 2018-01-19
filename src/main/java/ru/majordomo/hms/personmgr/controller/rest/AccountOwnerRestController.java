package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.event.account.AccountOwnerChangeEmailEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.QAccountOwner;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.MAILING_TYPE_INFO;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_NEWS;
import static ru.majordomo.hms.personmgr.common.Utils.getClientIP;

@RestController
@Validated
public class AccountOwnerRestController extends CommonRestController {

    private final AccountOwnerManager accountOwnerManager;

    @Autowired
    public AccountOwnerRestController(
            AccountOwnerManager accountOwnerManager
    ) {
        this.accountOwnerManager = accountOwnerManager;
    }

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountOwner> getOwner(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(account.getId());

        if (accountOwner == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(accountOwner, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.PUT)
    public ResponseEntity changeOwner(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody AccountOwner owner,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        boolean changeEmail = false;
        List<String> currentEmails = new ArrayList<>(currentOwner.getContactInfo().getEmailAddresses());
        if (authentication.getAuthorities().stream().noneMatch(ga -> ga.getAuthority().equals("UPDATE_CLIENT_CONTACTS"))) {
            changeEmail = !currentOwner.equalEmailAdressess(owner);
            accountOwnerManager.checkNotEmptyFields(currentOwner, owner);
            accountOwnerManager.setEmptyAndAllowedToEditFields(currentOwner, owner);
        } else {
            accountOwnerManager.setFields(currentOwner, owner);
        }

        accountOwnerManager.save(currentOwner);

        //Запишем инфу о произведенном изменении владельца в историю клиента
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();

        String ip = getClientIP(request);
        if (changeEmail) {
            PersonalAccount account = accountManager.findOne(accountId);
            Map<String, Object> paramsForToken = new HashMap<>();
            paramsForToken.put("newemails", owner.getContactInfo().getEmailAddresses());
            paramsForToken.put("ip", ip);
            paramsForToken.put("oldemails", currentEmails);
            publisher.publishEvent(new AccountOwnerChangeEmailEvent(account, paramsForToken));
        }
        String historyMessage = "Произведена смена владельца аккаунта с IP: " + ip + " Предыдущий владелец: " +
                currentOwner +
                " Новый владелец: " + owner
                ;
        if (changeEmail) {historyMessage += " Ожидается подтверждение смены контактных Email на "
                + owner.getContactInfo().getEmailAddresses();}
        params.put(HISTORY_MESSAGE_KEY, historyMessage);
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/account-owner", method = RequestMethod.GET)
    public ResponseEntity<List<AccountOwner>> listAll(
            @RequestParam(required = false) AccountOwner.Type type,
            @RequestParam(required = false) List<AccountOwner.Type> types,
            @RequestParam(required = false, defaultValue = MAILING_TYPE_INFO) String mailingType
    ) {
        List<AccountOwner> accountOwners;
        if (type != null) {
            accountOwners = accountOwnerManager.findAllByTypeIn(Collections.singletonList(type));
        } else if (types != null && !types.isEmpty()) {
            accountOwners = accountOwnerManager.findAllByTypeIn(types);
        } else {
            accountOwners = accountOwnerManager.findAll();
        }

        List<PersonalAccountWithNotificationsProjection> accounts = accountManager.findWithNotifications();
        Map<String, PersonalAccountWithNotificationsProjection> accountMap = accounts
                .stream()
                .collect(Collectors.toMap(PersonalAccountWithNotificationsProjection::getId, account -> account));

        if (mailingType.equals(MAILING_TYPE_INFO)) {
            accountOwners.removeIf(accountOwner -> {
                PersonalAccountWithNotificationsProjection account = accountMap.get(accountOwner.getPersonalAccountId());

                return account != null && !account.hasNotification(EMAIL_NEWS);
            });
        }

        accountOwners.forEach(accountOwner -> {
            PersonalAccountWithNotificationsProjection account = accountMap.get(accountOwner.getPersonalAccountId());
            accountOwner.setAccountId(account.getAccountId());
        });

        return new ResponseEntity<>(accountOwners, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @RequestMapping(value = "/account-owner/search", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountOwner>> search(
            @RequestParam("search") String search,
            Pageable pageable
    ) {
        QAccountOwner qAccountOwner = QAccountOwner.accountOwner;

        BooleanBuilder builder = new BooleanBuilder();
        Predicate predicate = builder
                .and(qAccountOwner.name.containsIgnoreCase(search))
                .or(qAccountOwner.personalAccountName.containsIgnoreCase(search))
                .or(qAccountOwner.contactInfo.emailAddresses.contains(search))
                .or(qAccountOwner.contactInfo.phoneNumbers.contains(search))
                .or(qAccountOwner.personalInfo.inn.eq(search))
                .or(qAccountOwner.personalInfo.number.containsIgnoreCase(search));

        Page<AccountOwner> accountOwners = accountOwnerManager.findAll(predicate, pageable);

        return new ResponseEntity<>(accountOwners, HttpStatus.OK);
    }
}