package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.seo.SeoOrderedEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;
import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.repository.AccountSeoOrderRepository;
import ru.majordomo.hms.personmgr.repository.SeoRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SEO_ORDER_CREATE;

@RestController
@RequestMapping("/{accountId}/seo")
@Validated
public class SeoRestController extends CommonRestController {
    private final AccountSeoOrderRepository accountSeoOrderRepository;
    private final SeoRepository seoRepository;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SeoRestController(
            AccountSeoOrderRepository accountSeoOrderRepository,
            SeoRepository seoRepository,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher
    ) {
        this.accountSeoOrderRepository = accountSeoOrderRepository;
        this.seoRepository = seoRepository;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<Seo>> getSeos(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<Seo> seos = seoRepository.findAll();

        return new ResponseEntity<>(seos, HttpStatus.OK);
    }

    @RequestMapping(value = "/order", method = RequestMethod.GET)
    public ResponseEntity<List<AccountSeoOrder>> getSeoOrder(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        List<AccountSeoOrder> orders = accountSeoOrderRepository.findByPersonalAccountId(account.getId());

        if(orders == null || orders.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @RequestMapping(value = "/order", method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> makeSeoOrder(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.SEO);

        Utils.checkRequiredParams(requestBody, ACCOUNT_SEO_ORDER_CREATE);

        String domainName = (String) requestBody.get("domainName");

        Seo seo;

        String seoTypeString = (String) requestBody.get("seoType");

        try {
            SeoType seoType = SeoType.valueOf(seoTypeString);
            seo = seoRepository.findByType(seoType);

            if(seo == null){
                throw new ParameterValidationException("Услуга SEO с типом '" + seoType + "' не найдена");
            }
        } catch (IllegalArgumentException e) {
            throw new ParameterValidationException("Тип услуги SEO должен быть одним из: " + Arrays.toString(SeoType.values()));
        }

        LocalDateTime now = LocalDateTime.now();
        now = now.minusDays(1L);

        List<AccountSeoOrder> orders = accountSeoOrderRepository.findByPersonalAccountIdAndDomainNameAndCreatedAfter(account.getId(), domainName, now);

        if(orders != null && !orders.isEmpty()) {
            if (orders.stream().anyMatch(accountSeoOrder -> accountSeoOrder.getSeo().getType() == seo.getType())) {
                throw new ParameterValidationException("Заказ на услугу SEO для домена '" + domainName + "' уже был получен ранее");
            }
        }

        accountHelper.checkBalance(account, seo.getService());

        AccountSeoOrder order = new AccountSeoOrder();
        order.setPersonalAccountId(account.getId());
        order.setCreated(LocalDateTime.now());
        order.setDomainName(domainName);
        order.setSeoId(seo.getId());

        accountSeoOrderRepository.save(order);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(seo.getService())
                .build();
        accountHelper.charge(account, chargeMessage);

        Map<String, String> params = new HashMap<>();
        params.put(DOMAIN_NAME_KEY, domainName);
        params.put(SERVICE_NAME_KEY, seo.getName());

        publisher.publishEvent(new SeoOrderedEvent(account.getId(), params));

        history.save(accountId, "Произведен заказ услуги '" + seo.getName() + "' для домена '" + domainName + "'", request);

        return new ResponseEntity<>(
                this.createSuccessResponse("Произведен заказ услуги '" + seo.getName() +
                        "' для домена '" + domainName + "'"),
                HttpStatus.OK
        );
    }
}