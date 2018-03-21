package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.dto.BitrixLicenseOrderRequest;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.repository.BitrixLicenseOrderRepository;
import ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager;
import ru.majordomo.hms.personmgr.service.order.BitrixLicenseProlongManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.NEW;
import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.PROLONG;

@RestController
public class BitrixLicenseOrderRestController extends CommonRestController {

    private BitrixLicenseOrderRepository repository;
    private BitrixLicenseOrderManager bitrixLicenseOrderManager;
    private BitrixLicenseProlongManager bitrixLicenseProlongManager;

    @Autowired
    public BitrixLicenseOrderRestController(
            BitrixLicenseOrderRepository repository,
            BitrixLicenseOrderManager bitrixLicenseOrderManager,
            BitrixLicenseProlongManager bitrixLicenseProlongManager
    ) {
        this.repository = repository;
        this.bitrixLicenseOrderManager = bitrixLicenseOrderManager;
        this.bitrixLicenseProlongManager = bitrixLicenseProlongManager;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @GetMapping("/{accountId}/bitrix-license-order/{orderId}")
    public ResponseEntity<BitrixLicenseOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder order = repository.findOneByIdAndPersonalAccountId(orderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @GetMapping("/{accountId}/bitrix-license-order")
    public ResponseEntity<Page<BitrixLicenseOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<BitrixLicenseOrder> orders = repository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @GetMapping("/bitrix-license-order")
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
    @PostMapping("/{accountId}/bitrix-license-order/{orderId}")
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

    @PostMapping("/{accountId}/bitrix-license-order")
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
        order.setType(NEW);

        bitrixLicenseOrderManager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{accountId}/bitrix-license-order/{orderId}/prolong")
    public ResponseEntity<Void> prolong(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder previousOrder = repository.findOneByIdAndPersonalAccountId(orderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        BitrixLicenseOrder order = new BitrixLicenseOrder();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setDomainName(previousOrder.getDomainName());
        order.setServiceId(previousOrder.getServiceId());
        order.setPreviousOrder(previousOrder);
        order.setType(PROLONG);

        bitrixLicenseProlongManager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}