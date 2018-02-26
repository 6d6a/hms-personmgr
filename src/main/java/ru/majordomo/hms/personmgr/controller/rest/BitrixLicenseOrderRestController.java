package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.dto.BitrixLicenseOrderRequest;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.order.QAccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.repository.BitrixLicenseOrderRepository;
import ru.majordomo.hms.personmgr.service.BitrixLicenseOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
public class BitrixLicenseOrderRestController extends CommonRestController {

    private BitrixLicenseOrderRepository repository;
    private BitrixLicenseOrderManager bitrixLicenseOrderManager;

    @Autowired
    public BitrixLicenseOrderRestController(
            BitrixLicenseOrderRepository repository,
            BitrixLicenseOrderManager bitrixLicenseOrderManager
    ) {
        this.repository = repository;
        this.bitrixLicenseOrderManager = bitrixLicenseOrderManager;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/bitrix-license-order/{orderId}",
            method = RequestMethod.GET)
    public ResponseEntity<BitrixLicenseOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder order = repository.findOneByIdAndPersonalAccountId(orderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/bitrix-license-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<BitrixLicenseOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<BitrixLicenseOrder> orders = repository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @RequestMapping(value = "/bitrix-license-order",
            method = RequestMethod.GET)
    public ResponseEntity<Page<BitrixLicenseOrder>> getAllOrders(
            Pageable pageable,
            @RequestParam Map<String, String> search
    ) {
        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        Page<BitrixLicenseOrder> orders;

        if (!accId.isEmpty()) {
            orders = repository.findByPersonalAccountId(accId, pageable);
        } else {
            orders = repository.findAll(pageable);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @RequestMapping(value = "/{accountId}/bitrix-license-order/{orderId}",
            method = RequestMethod.POST)
    public ResponseEntity<Void> update(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId,
            @RequestBody OrderState orderState,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder order = repository.findOneByIdAndPersonalAccountId(orderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        bitrixLicenseOrderManager.changeState(order, orderState, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/bitrix-license-order",
            method = RequestMethod.POST)
    public ResponseEntity<Void> create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody @Valid BitrixLicenseOrderRequest bitrixLicenseOrderRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        String operator = request.getUserPrincipal().getName();

        BitrixLicenseOrder order = new BitrixLicenseOrder();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setDomainName(bitrixLicenseOrderRequest.getDomainName());
        order.setServiceId(bitrixLicenseOrderRequest.getServiceId());

        bitrixLicenseOrderManager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
