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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateOrder.Type.NEW;
import static ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateOrder.Type.RENEW;

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

        orders.forEach(manager::build);

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
            @RequestBody SslCertificateOrder order,
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

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{accountId}/ssl-certificate-order/renew/{orderId}")
    public ResponseEntity<Void> renew(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(SslCertificateOrder.class) @PathVariable(value = "orderId") String orderId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String operator = request.getUserPrincipal().getName();

        SslCertificateOrder order = manager.findOneByIdAndPersonalAccountId(orderId, accountId);
        order.unSetId();
        order.setValidFrom(null);
        order.setValidTo(null);
        order.setCsr(null);
        order.setKey(null);
        order.setChain(null);
        order.setLastResponse(null);
        order.setVersion(null);
        order.setExternalOrderId(null);
        order.setExternalState(null);
        order.setDocumentNumber(null);
        order.setCreated(LocalDateTime.now());
        order.setUpdated(null);

        order.setOperator(operator);
        order.setOrderType(RENEW);

        Set<ConstraintViolation<@Valid SslCertificateOrder>> errors = validator.validate(order);
        if (errors != null && !errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }

        manager.create(order, operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}