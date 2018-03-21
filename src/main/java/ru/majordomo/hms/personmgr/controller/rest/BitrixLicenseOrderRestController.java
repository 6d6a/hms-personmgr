package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.dto.BitrixLicenseOrderRequest;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.order.QBitrixLicenseOrder;
import ru.majordomo.hms.personmgr.repository.BitrixLicenseOrderRepository;
import ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.NEW;
import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.PROLONG;

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
    @GetMapping("/{accountId}/bitrix-license-order/{orderId}")
    public ResponseEntity<BitrixLicenseOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder order = repository.findOneByIdAndPersonalAccountId(orderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

//    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
//    @GetMapping("/{accountId}/bitrix-license-order")
//    public ResponseEntity<Page<BitrixLicenseOrder>> getAll(
//            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
//            Pageable pageable
//    ) {
//        PersonalAccount account = accountManager.findOne(accountId);
//
//        Page<BitrixLicenseOrder> orders = repository.findByPersonalAccountId(account.getId(), pageable);
//
//        return new ResponseEntity<>(orders, HttpStatus.OK);
//    }

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

        bitrixLicenseOrderManager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/{accountId}/bitrix-license-order")
    public ResponseEntity<Page<BitrixLicenseOrder>> getExpiring(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(defaultValue = "") String search,
            Pageable pageable
    ) {
        Predicate predicate = bySearch(accountId, search);

        Page<BitrixLicenseOrder> orders = repository.findAll(predicate, pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    private Predicate bySearch(String personalAccountId, String search){
        QBitrixLicenseOrder qOrder = QBitrixLicenseOrder.bitrixLicenseOrder;

        BooleanBuilder builder = new BooleanBuilder()
                .and(qOrder.personalAccountId.eq(personalAccountId));

        switch (search) {
            case "expiring":
                builder = builder
                        .and(qOrder.state.eq(OrderState.FINISHED))
                        .and(qOrder.updated.after(LocalDateTime.now().minusDays(15)))
                        .and(qOrder.updated.before(LocalDateTime.now()))
                        .and(qOrder.id.notIn(bitrixLicenseOrderManager.getProlongedOrProlongingIds(personalAccountId)));
                break;

            case "expired":
                builder = builder
                        .and(qOrder.state.eq(OrderState.FINISHED))
                        .and(qOrder.updated.after(LocalDateTime.now()))
                        .and(qOrder.id.notIn(bitrixLicenseOrderManager.getProlongedOrProlongingIds(personalAccountId)));
                break;

            case "declined":
                builder = builder.and(qOrder.state.eq(OrderState.DECLINED));
                break;

            case "new":
                builder = builder.and(qOrder.state.eq(OrderState.NEW));
                break;

            case "in_progress":
                builder = builder.and(qOrder.state.eq(OrderState.IN_PROGRESS));
                break;
        }
        return builder.getValue();
    }
}