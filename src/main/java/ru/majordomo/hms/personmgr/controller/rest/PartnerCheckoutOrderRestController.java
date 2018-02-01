package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.repository.AccountPartnerCheckoutOrderRepository;
import ru.majordomo.hms.personmgr.service.PartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class PartnerCheckoutOrderRestController extends CommonRestController {

    private AccountPartnerCheckoutOrderRepository repository;
    private PartnerCheckoutOrder partnerCheckoutOrder;

    @Autowired
    public PartnerCheckoutOrderRestController(
            AccountPartnerCheckoutOrderRepository repository,
            PartnerCheckoutOrder partnerCheckoutOrder
    ) {
        this.repository = repository;
        this.partnerCheckoutOrder = partnerCheckoutOrder;
    }

    //TODO add routs to apigw

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

    //TODO add role for modify order
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

        partnerCheckoutOrder.setAccountOrder(partnerOrder);

        String operator = request.getUserPrincipal().getName();

        switch (orderState) {
            case FINISHED:
                partnerCheckoutOrder.finish(operator);
                break;
            case DECLINED:
                partnerCheckoutOrder.decline(operator);
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
        partnerOrder.setAmount(amount);

        partnerCheckoutOrder.setAccountOrder(partnerOrder);
        partnerCheckoutOrder.create(operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }


}
