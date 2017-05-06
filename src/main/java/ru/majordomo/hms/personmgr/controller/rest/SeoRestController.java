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
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.seo.SeoOrderedEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;
import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.repository.AccountSeoOrderRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.SeoRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validators.ObjectId;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SEO_ORDER_CREATE;

@RestController
@RequestMapping("/{accountId}/seo")
@Validated
public class SeoRestController extends CommonRestController {
    private final PersonalAccountRepository accountRepository;
    private final AccountSeoOrderRepository accountSeoOrderRepository;
    private final SeoRepository seoRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SeoRestController(
            PersonalAccountRepository accountRepository,
            AccountSeoOrderRepository accountSeoOrderRepository,
            SeoRepository seoRepository,
            RcUserFeignClient rcUserFeignClient,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher) {
        this.accountRepository = accountRepository;
        this.accountSeoOrderRepository = accountSeoOrderRepository;
        this.seoRepository = seoRepository;
        this.rcUserFeignClient = rcUserFeignClient;
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
        PersonalAccount account = accountRepository.findOne(accountId);

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
        PersonalAccount account = accountRepository.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SEO_ORDER_CREATE);

        String webSiteId = (String) requestBody.get("webSiteId");

        Seo seo;

        String seoTypeString = (String) requestBody.get("seoType");

        try {
            SeoType seoType = SeoType.valueOf(seoTypeString);
            seo = seoRepository.findByType(seoType);

            if(seo == null){
                return new ResponseEntity<>(
                        this.createErrorResponse("Seo with type " + seoType + " not found"),
                        HttpStatus.BAD_REQUEST
                );
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    this.createErrorResponse("seoType from requestBody must be one of: " +
                    Arrays.toString(SeoType.values())),
                    HttpStatus.BAD_REQUEST
            );
        }

        LocalDateTime now = LocalDateTime.now();
        now = now.minusDays(1L);

        AccountSeoOrder order = accountSeoOrderRepository.findByPersonalAccountIdAndWebSiteIdAndCreatedAfter(account.getId(), webSiteId, now);

        if(order != null && order.getSeo().getType() == seo.getType()){
            return new ResponseEntity<>(
                    this.createErrorResponse("AccountSeoOrder already found for specified websiteId " + webSiteId),
                    HttpStatus.BAD_REQUEST
            );
        }

        WebSite webSite = null;

        try {
            webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.controller.rest.SeoRestController.makeSeoOrder " + e.getMessage());
        }

        if (webSite == null) {
            return new ResponseEntity<>(this.createErrorResponse("WebSite with id " + webSiteId +
                    " not found"), HttpStatus.BAD_REQUEST);
        }

        accountHelper.checkBalance(account, seo.getService());

        accountHelper.charge(account, seo.getService());

        order = new AccountSeoOrder();
        order.setPersonalAccountId(account.getId());
        order.setCreated(LocalDateTime.now());
        order.setWebSiteId(webSiteId);
        order.setSeoId(seo.getId());

        accountSeoOrderRepository.save(order);

        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_ID_KEY, webSiteId);
        params.put(SERVICE_NAME_KEY, seo.getName());

        logger.debug("Trying to publish SeoOrderedEvent publisher: " + publisher.toString() + " " + publisher.getClass());
        publisher.publishEvent(new SeoOrderedEvent(account, params));
        logger.debug("Published SeoOrderedEvent");

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> paramsHistory = new HashMap<>();
        paramsHistory.put(HISTORY_MESSAGE_KEY, "Произведен заказ услуги '" + seo.getName() + "' для сайта '" + webSite.getName() + "'");
        paramsHistory.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, paramsHistory));

        return new ResponseEntity<>(
                this.createSuccessResponse("AccountSeoOrder created for websiteId " + webSiteId),
                HttpStatus.OK
        );
    }
}