package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.model.order.QAccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.repository.AccountPartnerCheckoutOrderRepository;
import ru.majordomo.hms.personmgr.service.PartnerCheckoutOrderManager;
import ru.majordomo.hms.personmgr.service.PartnerCheckoutOrderMangerFactory;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class PartnerCheckoutOrderRestController extends CommonRestController {

    private AccountPartnerCheckoutOrderRepository repository;
    private PartnerCheckoutOrderMangerFactory partnerCheckoutOrderMangerFactory;
    private PersonalAccountManager personalAccountManager;

    @Autowired
    public PartnerCheckoutOrderRestController(
            AccountPartnerCheckoutOrderRepository repository,
            PartnerCheckoutOrderMangerFactory partnerCheckoutOrderMangerFactory,
            PersonalAccountManager personalAccountManager
    ) {
        this.repository = repository;
        this.partnerCheckoutOrderMangerFactory = partnerCheckoutOrderMangerFactory;
        this.personalAccountManager = personalAccountManager;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/partner-checkout-order/{partnerCheckoutOrderId}",
            method = RequestMethod.GET)
    public ResponseEntity<AccountPartnerCheckoutOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountPartnerCheckoutOrder.class) @PathVariable(value = "partnerCheckoutOrderId") String partnerCheckoutOrderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountPartnerCheckoutOrder partnerOrder = repository.findOneByIdAndPersonalAccountId(partnerCheckoutOrderId, account.getId());

        return new ResponseEntity<>(partnerOrder, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/partner-checkout-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<AccountPartnerCheckoutOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<AccountPartnerCheckoutOrder> partnerOrders = repository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(partnerOrders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/partner-checkout-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<AccountPartnerCheckoutOrder>> getAllOrders(
            Pageable pageable,
            @RequestParam Map<String, String> search
    ) {
        QAccountPartnerCheckoutOrder qAccountAccountPartnerCheckoutOrder = QAccountPartnerCheckoutOrder.accountPartnerCheckoutOrder;

        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        BooleanBuilder builder = new BooleanBuilder();

        Predicate predicate = builder.and(
                accId.isEmpty() ? null : qAccountAccountPartnerCheckoutOrder.personalAccountId.equalsIgnoreCase(accId)
        );

        Page<AccountPartnerCheckoutOrder> partnerOrders = repository.findAll(predicate, pageable);

        return new ResponseEntity<>(partnerOrders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/partner-checkout-order/{partnerCheckoutOrderId}",
            method = RequestMethod.POST)
    public ResponseEntity<Void> update(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountPartnerCheckoutOrder.class) @PathVariable(value = "partnerCheckoutOrderId") String partnerCheckoutOrderId,
            @RequestBody OrderState orderState,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountPartnerCheckoutOrder partnerOrder = repository.findOneByIdAndPersonalAccountId(partnerCheckoutOrderId, account.getId());

        PartnerCheckoutOrderManager partnerCheckoutOrderManager = partnerCheckoutOrderMangerFactory.createManager(partnerOrder);

        String operator = request.getUserPrincipal().getName();

        switch (orderState) {
            case FINISHED:
                partnerCheckoutOrderManager.finish(operator);
                break;
            case DECLINED:
                partnerCheckoutOrderManager.decline(operator);
                break;
            case IN_PROGRESS:
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/partner-checkout-order",
            method = RequestMethod.POST)
    public ResponseEntity<Void> create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody BigDecimal amount,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        String operator = request.getUserPrincipal().getName();

        AccountPartnerCheckoutOrder partnerOrder = new AccountPartnerCheckoutOrder();
        partnerOrder.setPersonalAccountId(account.getId());
        partnerOrder.setPersonalAccountName(account.getName());
        partnerOrder.setAmount(amount);

        PartnerCheckoutOrderManager partnerCheckoutOrderManager = partnerCheckoutOrderMangerFactory.createManager(partnerOrder);
        partnerCheckoutOrderManager.create(operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private  String getAccountIdFromNameOrAccountId(String accountId) {
        String personalAccountId = accountId;

        if (accountId != null && !accountId.isEmpty()){

            accountId = accountId.replaceAll("[^0-9]", "");
            try {
                PersonalAccount account = personalAccountManager.findByAccountId(accountId);
                if (account != null) {
                    personalAccountId = account.getId();
                }
            } catch (ResourceNotFoundException e) {
                return personalAccountId;
            }
        }
        return personalAccountId;
    }

}
