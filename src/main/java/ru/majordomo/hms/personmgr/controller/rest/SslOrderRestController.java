package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.ssl.*;
import ru.majordomo.hms.personmgr.service.order.ssl.SSLOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateOrder.Type.NEW;

@Validated
@RestController
public class SslOrderRestController extends CommonRestController {

    private final SSLOrderManager manager;
    private final EntityBuilder<SslCertificateProduct> productEntityBuilder;
    private final EntityBuilder<SslCertificateApproverEmail> approverEmailEntityBuilder;
    private final EntityBuilder<Country> countryEntityBuilder;
    private final EntityBuilder<SslCertificateServerType> serverTypeEntityBuilder;
    private final Validator validator;

    @Autowired
    public SslOrderRestController(
            SSLOrderManager manager,
            EntityBuilder<SslCertificateProduct> productEntityBuilder,
            EntityBuilder<SslCertificateApproverEmail> approverEmailEntityBuilder,
            EntityBuilder<Country> countryEntityBuilder,
            EntityBuilder<SslCertificateServerType> serverTypeEntityBuilder,
            Validator validator
    ) {
        this.manager = manager;
        this.productEntityBuilder = productEntityBuilder;
        this.approverEmailEntityBuilder = approverEmailEntityBuilder;
        this.countryEntityBuilder = countryEntityBuilder;
        this.serverTypeEntityBuilder = serverTypeEntityBuilder;
        this.validator = validator;
    }

    @GetMapping({"/{accountId}/ssl-certificate-order/product", "/ssl-certificate-order/product"})
    public List<SslCertificateProduct> getProducts() {
        return productEntityBuilder.findAll();
    }

    @GetMapping({"/{accountId}/ssl-certificate-order/approver-emails", "/ssl-certificate-order/approver-emails"})
    public List<SslCertificateApproverEmail> getApproverEmails() {
        return approverEmailEntityBuilder.findAll();
    }

    @GetMapping({"/{accountId}/ssl-certificate-order/country", "/ssl-certificate-order/country"})
    public List<Country> getCountries() {
        return countryEntityBuilder.findAll();
    }

    @GetMapping({"/{accountId}/ssl-certificate-order/server-type", "/ssl-certificate-order/server-type"})
    public List<SslCertificateServerType> getServerTypes() {
        return serverTypeEntityBuilder.findAll();
    }

    @PreAuthorize("hasAuthority('ACCOUNT_SSL_CERTIFICATE_ORDER_VIEW')")
    @GetMapping("/{accountId}/ssl-certificate-order/{orderId}")
    public ResponseEntity<SslCertificateOrder> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(SslCertificateOrder.class) @PathVariable(value = "orderId") String orderId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        SslCertificateOrder order = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

//    @PreAuthorize("hasAuthority('ACCOUNT_SSL_CERTIFICATE_ORDER_VIEW')")
    @GetMapping("/{accountId}/ssl-certificate-order")
    public ResponseEntity<Page<SslCertificateOrder>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<SslCertificateOrder> orders = manager.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_SSL_CERTIFICATE_ORDER_VIEW')")
    @GetMapping("/ssl-certificate-order")
    public ResponseEntity<Page<SslCertificateOrder>> getAllOrders(
            Pageable pageable,
            @RequestParam Map<String, String> search
    ) {
        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        Page<SslCertificateOrder> orders;

        if (!accId.isEmpty()) {
            orders = manager.findByPersonalAccountId(accId, pageable);
        } else {
            orders = manager.findAll(pageable);
            orders.forEach(manager::build);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_SSL_CERTIFICATE_ORDER_VIEW')")
    @PostMapping("/{accountId}/ssl-certificate-order/{orderId}")
    public ResponseEntity<Void> update(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(SslCertificateOrder.class) @PathVariable(value = "orderId") String orderId,
            @RequestBody OrderState orderState,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        SslCertificateOrder order = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());

        String operator = request.getUserPrincipal().getName();

        manager.changeState(order, orderState, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{accountId}/ssl-certificate-order")
    public ResponseEntity<Void> create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody /*@Valid */SslCertificateOrder order,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        String operator = request.getUserPrincipal().getName();

        order.unSetId();
        order.setPersonalAccountId(account.getId());
        order.setPersonalAccountName(account.getName());
        order.setOrderType(NEW);
        order.setOperator(operator);
        order.setState(OrderState.NEW);

        Set<ConstraintViolation<@Valid SslCertificateOrder>> errors = validator.validate(order);
        if (errors != null && !errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }

        manager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.OK);
    }

//    @PostMapping("/{accountId}/ssl-certificate-order/{orderId}/prolong")
//    public ResponseEntity<Void> prolong(
//            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
//            @ObjectId(SslCertificateOrder.class) @PathVariable(value = "orderId") String orderId,
//            SecurityContextHolderAwareRequestWrapper request
//    ) {
//        PersonalAccount account = accountManager.findOne(accountId);
//
//        SslCertificateOrder previousOrder = manager.findOneByIdAndPersonalAccountId(orderId, account.getId());
//
//        String operator = request.getUserPrincipal().getName();
//
//        SslCertificateOrder order = new SslCertificateOrder();
//        order.setPersonalAccountId(account.getId());
//        order.setPersonalAccountName(account.getName());
//        order.setDomainName(previousOrder.getDomainName());
//        order.setServiceId(previousOrder.getServiceId());
//        order.setPreviousOrder(previousOrder);
//        order.setPreviousOrderId(orderId);
//        order.setType(PROLONG);
//
//        manager.create(order, operator);
//
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }

//    @JsonView(Views.Public.class)
//    @GetMapping("/{accountId}/ssl-certificate-order/search")
//    public ResponseEntity<Page<SslCertificateOrder>> getExpiring(
//            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
//            @RequestParam(required = false) Map<String, String> params,
//            Pageable pageable
//    ) {
//        Predicate predicate = getPredicate(accountId, params);
//
//        Page<SslCertificateOrder> orders = manager.findAll(predicate, pageable);
//
//        orders.forEach(manager::build);
//
//        return new ResponseEntity<>(orders, HttpStatus.OK);
//    }

//    private Predicate getPredicate(String personalAccountId, Map<String, String> params){
//        LocalDateTime updatedAfter;
//        LocalDateTime updatedBefore;
//        OrderState state;
//        String domain = params.getOrDefault("domain", null);
//        boolean excludeProlongedOrders = Boolean.valueOf(params.getOrDefault("excludeProlonged", null));
//
//        try {
//            String afterString = params.getOrDefault("after", "");
//            updatedAfter = afterString.isEmpty() ? null : LocalDateTime.of(LocalDate.parse(afterString, DateTimeFormatter.ISO_DATE), LocalTime.MIN);
//
//            String beforeString = params.getOrDefault("before", "");
//            updatedBefore = beforeString.isEmpty() ? null : LocalDateTime.of(LocalDate.parse(beforeString, DateTimeFormatter.ISO_DATE), LocalTime.MIN);
//
//            String stateString = params.getOrDefault("state", "");
//            state = stateString.isEmpty() ? null : OrderState.valueOf(stateString.toUpperCase());
//
//            switch (params.getOrDefault("preset", "")) {
//                case "expiring":
//                    updatedAfter = LocalDateTime.now().minusYears(1);
//                    updatedBefore = updatedAfter.plusDays(MAY_PROLONG_DAYS_BEFORE_EXPIRED);
//                    state = OrderState.FINISHED;
//                    excludeProlongedOrders = true;
//
//                    break;
//                case "expired":
//                    updatedBefore = LocalDateTime.now().minusYears(1);
//                    updatedAfter = updatedBefore.minusMonths(3);
//                    state = OrderState.FINISHED;
//                    excludeProlongedOrders = true;
//
//                    break;
//                case "declined":
//                    state = OrderState.DECLINED;
//
//                    break;
//                case "new":
//                    state = OrderState.NEW;
//
//                    break;
//                case "in_progress":
//                    state = OrderState.IN_PROGRESS;
//
//                    break;
//            }
//        } catch (DateTimeParseException e) {
//            throw new ParameterValidationException("Некорректный формат даты, необходимо указывать дату в формате 'YYYY-MM-DD'");
//        } catch (IllegalArgumentException e) {
//            throw new ParameterValidationException("state должен быть одним из " + Arrays.asList(OrderState.values()));
//        }
//        return manager.getPredicate(personalAccountId, updatedAfter, updatedBefore, state, domain, excludeProlongedOrders);
//    }
}