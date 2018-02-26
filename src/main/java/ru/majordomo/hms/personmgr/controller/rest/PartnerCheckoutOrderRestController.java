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
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.model.order.QAccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.repository.AccountPartnerCheckoutOrderRepository;
import ru.majordomo.hms.personmgr.service.PartnerCheckoutOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class PartnerCheckoutOrderRestController extends CommonRestController {

    private AccountPartnerCheckoutOrderRepository repository;
    private PartnerCheckoutOrderManager partnerCheckoutOrderManager;

    @Autowired
    public PartnerCheckoutOrderRestController(
            AccountPartnerCheckoutOrderRepository repository,
            PartnerCheckoutOrderManager partnerCheckoutOrderManager
    ) {
        this.repository = repository;
        this.partnerCheckoutOrderManager = partnerCheckoutOrderManager;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/partner-checkout-order/{partnerCheckoutOrderId}",
            method = RequestMethod.GET)
    public ResponseEntity<AccountPartnerCheckoutOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountPartnerCheckoutOrder.class) @PathVariable(value = "partnerCheckoutOrderId") String partnerCheckoutOrderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountPartnerCheckoutOrder order = repository.findOneByIdAndPersonalAccountId(partnerCheckoutOrderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/partner-checkout-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<AccountPartnerCheckoutOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<AccountPartnerCheckoutOrder> orders = repository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PARTNER_ORDER_VIEW')")
    @RequestMapping(value = "/partner-checkout-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<AccountPartnerCheckoutOrder>> getAllOrders(
            Pageable pageable,
            @RequestParam Map<String, String> search
    ) {
        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        Page<AccountPartnerCheckoutOrder> orders;

        if (!accId.isEmpty()) {
            orders = repository.findByPersonalAccountId(accId, pageable);
        } else {
            orders = repository.findAll(pageable);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
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

        AccountPartnerCheckoutOrder order = repository.findOneByIdAndPersonalAccountId(partnerCheckoutOrderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        partnerCheckoutOrderManager.changeState(order, orderState, operator);

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

        AccountPartnerCheckoutOrder order = new AccountPartnerCheckoutOrder();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setAmount(amount);

        partnerCheckoutOrderManager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
