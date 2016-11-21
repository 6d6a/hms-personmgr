package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;
import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.repository.AccountSeoOrderRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.SeoRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RestController
@RequestMapping({"/seo", "/{accountId}/seo"})
public class RestSeoController extends CommonRestController {

    @Autowired
    private PersonalAccountRepository accountRepository;

    @Autowired
    private AccountSeoOrderRepository accountSeoOrderRepository;

    @Autowired
    private SeoRepository seoRepository;

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private RcUserFeignClient rcUserFeignClient;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<Seo>> getAccount(@PathVariable(value = "accountId") String accountId) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<Seo> seos = seoRepository.findAll();

        return new ResponseEntity<>(seos, HttpStatus.OK);
    }

    @RequestMapping(value = "/order", method = RequestMethod.GET)
    public ResponseEntity<List<AccountSeoOrder>> getAccountPlan(@PathVariable(value = "accountId") String accountId) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<AccountSeoOrder> orders = accountSeoOrderRepository.findByPersonalAccountId(account.getId());

        if(orders == null || orders.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @RequestMapping(value = "/order", method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> changeAccountPlan(@PathVariable(value = "accountId") String accountId, @RequestBody Map<String, String> requestBody) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        String webSiteId = requestBody.get("webSiteId");

        if(webSiteId == null){
            return new ResponseEntity<>(this.createErrorResponse("webSiteId not found in requestBody"), HttpStatus.BAD_REQUEST);
        }

        Seo seo;

        String seoTypeString = requestBody.get("seoType");

        if(seoTypeString == null){
            return new ResponseEntity<>(this.createErrorResponse("seoType not found in requestBody"), HttpStatus.BAD_REQUEST);
        }

        try {
            SeoType seoType = SeoType.valueOf(seoTypeString);
            seo = seoRepository.findByType(seoType);

            if(seo == null){
                return new ResponseEntity<>(this.createErrorResponse("Seo with type " + seoType + " not found"), HttpStatus.BAD_REQUEST);
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(this.createErrorResponse("seoType from requestBody must be one of: " + Arrays.toString(SeoType.values())), HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        now = now.minusDays(1L);

        AccountSeoOrder order = accountSeoOrderRepository.findByPersonalAccountIdAndWebSiteIdAndCreatedAfter(account.getId(), webSiteId, now);

        if(order != null && order.getSeo().getType() == seo.getType()){
            return new ResponseEntity<>(this.createErrorResponse("AccountSeoOrder already found for specified websiteId " + webSiteId), HttpStatus.BAD_REQUEST);
        }

        WebSite webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
        if (webSite == null) {
            return new ResponseEntity<>(this.createErrorResponse("WebSite with id " + webSiteId + " not found"), HttpStatus.BAD_REQUEST);
        }

        //TODO Списать деньгов с аккаунта

        order = new AccountSeoOrder();
        order.setPersonalAccountId(account.getId());
        order.setCreated(LocalDateTime.now());
        order.setWebSiteId(webSiteId);
        order.setSeoId(seo.getId());

        accountSeoOrderRepository.save(order);

        //Письмо в  СЕО
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        //TODO Поставить pro@
        String email = "web-script@majordomo.ru";
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        String clientEmails = "";
        String webSiteName = webSite.getName();

        Person person = rcUserFeignClient.getPersonOwner(account.getId());
        if (person != null) {
            clientEmails = String.join(", ", person.getEmailAddresses());
        }

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>2. E-mail: " + clientEmails + "<br>3. Имя сайта: " + webSiteName + "<br><br>" + "Услуга " + seo.getName() + " оплачена из ПУ.");
        parameters.put("subject", "Услуга " + seo.getName() + " оплачена");

        message.addParam("parametrs", parameters);

        businessActionBuilder.build(BusinessActionType.SEO_ORDER_MM, message);

        return new ResponseEntity<>(this.createSuccessResponse("AccountSeoOrder created for websiteId " + webSiteId), HttpStatus.OK);
    }
}