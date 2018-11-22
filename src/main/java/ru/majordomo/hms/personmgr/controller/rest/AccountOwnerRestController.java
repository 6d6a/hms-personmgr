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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.event.account.AccountOwnerChangeEmailEvent;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.QAccountOwner;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;
import ru.majordomo.hms.personmgr.service.JongoManager;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static java.util.stream.Collectors.toList;
import static ru.majordomo.hms.personmgr.common.Constants.MAILING_TYPE_INFO;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_NEWS;
import static ru.majordomo.hms.personmgr.common.Utils.getClientIP;

@RestController
@Validated
public class AccountOwnerRestController extends CommonRestController {
    private final AccountOwnerManager accountOwnerManager;
    private final RcUserFeignClient rcUserFeignClient;
    private final JongoManager jongoManager;

    @Autowired
    public AccountOwnerRestController(
            AccountOwnerManager accountOwnerManager,
            RcUserFeignClient rcUserFeignClient,
            JongoManager jongoManager
    ) {
        this.accountOwnerManager = accountOwnerManager;
        this.rcUserFeignClient = rcUserFeignClient;
        this.jongoManager = jongoManager;
    }

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountOwner> getOwner(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        AccountOwner accountOwner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        if (accountOwner == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(accountOwner, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.PUT)
    public ResponseEntity<AccountOwner> changeOwner(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody AccountOwner owner,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        AccountOwner currentOwner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        boolean changeEmail = false;

        String diffMessage = currentOwner.getDiffMessage(owner);

        List<String> currentEmails = new ArrayList<>(currentOwner.getContactInfo().getEmailAddresses());
        if (authentication.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("UPDATE_CLIENT_CONTACTS"))) {
            accountOwnerManager.setFields(currentOwner, owner);
        } else {
            changeEmail = !currentOwner.equalEmailAdressess(owner);
            accountOwnerManager.checkNotEmptyFields(currentOwner, owner);
            accountOwnerManager.setEmptyAndAllowedToEditFields(currentOwner, owner);
        }

        AccountOwner savedOwner = accountOwnerManager.save(currentOwner);

        //Запишем инфу о произведенном изменении владельца в историю клиента
        String operator = request.getUserPrincipal().getName();

        String ip = getClientIP(request);
        if (changeEmail) {
            PersonalAccount account = accountManager.findOne(accountId);
            Map<String, Object> paramsForToken = new HashMap<>();
            paramsForToken.put("newemails", owner.getContactInfo().getEmailAddresses());
            paramsForToken.put("ip", ip);
            paramsForToken.put("oldemails", currentEmails);
            publisher.publishEvent(new AccountOwnerChangeEmailEvent(account, paramsForToken));
        }

        String historyMessage = "С IP " + ip +  " изменены данные владельца аккаунта: " + diffMessage;

        if (changeEmail) {
            historyMessage += " Ожидается подтверждение смены контактных Email на " + owner.getContactInfo().getEmailAddresses();
        }

        history.save(accountId, historyMessage, operator);

        return new ResponseEntity<>(savedOwner, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/account-owner", method = RequestMethod.GET)
    public ResponseEntity<List<AccountOwner>> listAll(
            @RequestParam(required = false) AccountOwner.Type type,
            @RequestParam(required = false) List<AccountOwner.Type> types,
            @RequestParam(required = false, defaultValue = MAILING_TYPE_INFO) String mailingType,
            @RequestParam(required = false) List<String> sharedHostingServerIds,
            @RequestParam(required = false) Boolean onlyActive
    ) {
        logger.debug("[start]");
        List<AccountOwner> accountOwners = new ArrayList<>();

        if (type != null) {
            accountOwners = accountOwnerManager.findAllByTypeIn(Collections.singletonList(type));
        } else if (types != null && !types.isEmpty()) {
            accountOwners = accountOwnerManager.findAllByTypeIn(types);
        } else if (sharedHostingServerIds != null && !sharedHostingServerIds.isEmpty()) {
            List<AccountOwner> finalAccountOwners = accountOwners;
            sharedHostingServerIds.parallelStream().forEach(serverId -> {
                Map<String, String> filterParams = new HashMap<>();
                filterParams.put(SERVER_ID_KEY, serverId);

                finalAccountOwners.addAll(
                        rcUserFeignClient.filterUnixAccounts(filterParams)
                                .stream()
                                .map(unixAccount -> accountOwnerManager.findOneByPersonalAccountId(unixAccount.getAccountId()))
                                .filter(Objects::nonNull)
                                .collect(toList())
                );
            });
        } else {
            accountOwners = accountOwnerManager.findAll();
        }

        logger.debug("[ownersLoaded] " + accountOwners.size());

        Map<String, PersonalAccountWithNotificationsProjection> accountMap = jongoManager.getAccountsWithNotifications();

        logger.debug("[accountsLoaded] accountMap: " + accountMap.size());

        if (onlyActive != null && onlyActive) {
            accountOwners.removeIf(accountOwner -> {
                PersonalAccountWithNotificationsProjection account = accountMap.get(accountOwner.getPersonalAccountId());

                return account == null || !account.isActive();
            });
        }

        logger.debug("[notActiveChecked] " + accountOwners.size());

        if (mailingType.equals(MAILING_TYPE_INFO)) {
            accountOwners.removeIf(accountOwner -> {
                PersonalAccountWithNotificationsProjection account = accountMap.get(accountOwner.getPersonalAccountId());

                return account == null || !account.hasNotification(EMAIL_NEWS);
            });
        }

        logger.debug("[mailingTypeChecked] " + accountOwners.size());

        accountOwners.forEach(accountOwner -> {
            PersonalAccountWithNotificationsProjection account = accountMap.get(accountOwner.getPersonalAccountId());
            accountOwner.setAccountId(account.getAccountId());
        });

        logger.debug("[accountIdSet] " + accountOwners.size());

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
                .or(qAccountOwner.contactInfo.emailAddresses.any().containsIgnoreCase(search))
                .or(qAccountOwner.contactInfo.phoneNumbers.contains(search))
                .or(qAccountOwner.personalInfo.inn.eq(search))
                .or(qAccountOwner.personalInfo.number.containsIgnoreCase(search));

        Page<AccountOwner> accountOwners = accountOwnerManager.findAll(predicate, pageable);

        return new ResponseEntity<>(accountOwners, HttpStatus.OK);
    }
}