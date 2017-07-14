package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.event.account.AccountOwnerChangeEmailEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
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
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        boolean changeEmail = false;
        List<String> currentEmails = new ArrayList<>(currentOwner.getContactInfo().getEmailAddresses());
        if (!request.isUserInRole("ADMIN")) {
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
            @RequestParam(required = false) List<AccountOwner.Type> types
    ) {
        List<AccountOwner> accountOwners;
        if (type != null) {
            accountOwners = accountOwnerManager.findAllByTypeIn(Collections.singletonList(type));
        } else if (types != null && !types.isEmpty()) {
            accountOwners = accountOwnerManager.findAllByTypeIn(types);
        } else {
            accountOwners = accountOwnerManager.findAll();
        }

        return new ResponseEntity<>(accountOwners, HttpStatus.OK);
    }
}