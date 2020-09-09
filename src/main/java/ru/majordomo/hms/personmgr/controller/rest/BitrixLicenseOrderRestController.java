package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.dto.BitrixLicenseOrderRequest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.NEW;
import static ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder.LicenseType.PROLONG;
import static ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager.MAY_PROLONG_DAYS_BEFORE_EXPIRED;

@RestController
public class BitrixLicenseOrderRestController extends CommonRestController {

    private BitrixLicenseOrderManager manager;
    private AccountHelper accountHelper;

    @Autowired
    public BitrixLicenseOrderRestController(
            BitrixLicenseOrderManager manager,
            AccountHelper accountHelper
    ) {
        this.manager = manager;
        this.accountHelper = accountHelper;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @GetMapping("/{accountId}/bitrix-license-order/{orderId}")
    public ResponseEntity<BitrixLicenseOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder order = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @GetMapping("/{accountId}/bitrix-license-order")
    public ResponseEntity<Page<BitrixLicenseOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<BitrixLicenseOrder> orders = manager.findByPersonalAccountId(account.getId(), pageable);

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
            orders = manager.findByPersonalAccountId(accId, pageable);
        } else {
            orders = manager.findAll(pageable);
            orders.forEach(manager::mongoAfterConvert);
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

        BitrixLicenseOrder order = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        manager.changeState(order, orderState, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_BITRIX_LICENSE_ORDER_VIEW')")
    @PatchMapping("/{accountId}/bitrix-license-order/{orderId}/comment")
    public ResponseEntity<Void> update(
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable String orderId,
            @RequestBody Map<String, Object> params
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        BitrixLicenseOrder order = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());
        manager.updateComment(order, (String) params.get("comment"));
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/{accountId}/bitrix-license-order")
    public ResponseEntity<Void> create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody @Valid BitrixLicenseOrderRequest bitrixLicenseOrderRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        accountHelper.checkIsCmsAllowed(account);
        String operator = request.getUserPrincipal().getName();

        BitrixLicenseOrder order = new BitrixLicenseOrder();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setDomainName(bitrixLicenseOrderRequest.getDomainName());
        order.setServiceId(bitrixLicenseOrderRequest.getServiceId());
        order.setType(NEW);

        manager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{accountId}/bitrix-license-order/{orderId}/prolong")
    public ResponseEntity<Void> prolong(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(BitrixLicenseOrder.class) @PathVariable(value = "orderId") String orderId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        BitrixLicenseOrder previousOrder = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        BitrixLicenseOrder order = new BitrixLicenseOrder();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setDomainName(previousOrder.getDomainName());
        order.setServiceId(previousOrder.getServiceId());
        order.setPreviousOrder(previousOrder);
        order.setPreviousOrderId(orderId);
        order.setType(PROLONG);

        manager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/{accountId}/bitrix-license-order/search")
    public ResponseEntity<Page<BitrixLicenseOrder>> getExpiring(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(required = false) Map<String, String> params,
            Pageable pageable
    ) {
        Predicate predicate = getPredicate(accountId, params);

        Page<BitrixLicenseOrder> orders = manager.findAll(predicate, pageable);

        orders.forEach(manager::mongoAfterConvert);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    private Predicate getPredicate(String personalAccountId, Map<String, String> params){
        LocalDateTime updatedAfter;
        LocalDateTime updatedBefore;
        OrderState state;
        String domain = params.getOrDefault("domain", null);
        boolean excludeProlongedOrders = Boolean.valueOf(params.getOrDefault("excludeProlonged", null));

        try {
            String afterString = params.getOrDefault("after", "");
            updatedAfter = afterString.isEmpty() ? null : LocalDateTime.of(LocalDate.parse(afterString, DateTimeFormatter.ISO_DATE), LocalTime.MIN);

            String beforeString = params.getOrDefault("before", "");
            updatedBefore = beforeString.isEmpty() ? null : LocalDateTime.of(LocalDate.parse(beforeString, DateTimeFormatter.ISO_DATE), LocalTime.MIN);

            String stateString = params.getOrDefault("state", "");
            state = stateString.isEmpty() ? null : OrderState.valueOf(stateString.toUpperCase());

            switch (params.getOrDefault("preset", "")) {
                case "expiring":
                    updatedAfter = LocalDateTime.now().minusYears(1);
                    updatedBefore = updatedAfter.plusDays(MAY_PROLONG_DAYS_BEFORE_EXPIRED);
                    state = OrderState.FINISHED;
                    excludeProlongedOrders = true;

                    break;
                case "expired":
                    updatedBefore = LocalDateTime.now().minusYears(1);
                    updatedAfter = updatedBefore.minusMonths(3);
                    state = OrderState.FINISHED;
                    excludeProlongedOrders = true;

                    break;
                case "declined":
                    state = OrderState.DECLINED;

                    break;
                case "new":
                    state = OrderState.NEW;

                    break;
                case "in_progress":
                    state = OrderState.IN_PROGRESS;

                    break;
            }
        } catch (DateTimeParseException e) {
            throw new ParameterValidationException("Некорректный формат даты, необходимо указывать дату в формате 'YYYY-MM-DD'");
        } catch (IllegalArgumentException e) {
            throw new ParameterValidationException("state должен быть одним из " + Arrays.asList(OrderState.values()));
        }
        return manager.getPredicate(personalAccountId, updatedAfter, updatedBefore, state, domain, excludeProlongedOrders);
    }
}