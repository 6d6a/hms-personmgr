package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.revisium.RevisiumRequestBody;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.Valid;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
public class RevisiumRequestRestController extends CommonRestController {

    private final RevisiumRequestRepository revisiumRequestRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;
    private final PersonalAccountManager personalAccountManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;

    @Autowired
    public RevisiumRequestRestController(
            RevisiumRequestRepository revisiumRequestRepository,
            PersonalAccountManager personalAccountManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository
    ) {
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.personalAccountManager = personalAccountManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
    }

    //Список всех услуг Ревизиума
    @GetMapping("/{accountId}/revisium/services")
    public ResponseEntity<List<RevisiumRequestService>> listAllServices(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<RevisiumRequestService> revisiumRequestServices = revisiumRequestServiceRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(revisiumRequestServices, HttpStatus.OK);
    }

    //Одна услуга Ревизиума
    @GetMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}")
    public ResponseEntity<RevisiumRequestService> getOneService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId
    ) {
        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findByPersonalAccountIdAndId(
                accountId,
                revisiumRequestServiceId
        );

        return new ResponseEntity<>(revisiumRequestService, HttpStatus.OK);
    }

    //Все УСПЕШНЫЕ реквесты в Ревизиум ЗА ПОСЛЕДНИЙ ГОД
    @GetMapping("/{accountId}/revisium/requests")
    public ResponseEntity<List<RevisiumRequest>> listAllRequests(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<RevisiumRequest> revisiumRequests = revisiumRequestRepository.findByPersonalAccountIdAndSuccessGetResultAndCreatedAfter(
                accountId,
                true,
                LocalDateTime.now().minusYears(1L)
        );

        return new ResponseEntity<>(revisiumRequests, HttpStatus.OK);
    }

    //Все УСПЕШНЫЕ реквесты в Ревизиум по одному сайту ЗА ПОСЛЕДНИЙ ГОД
    @GetMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}/requests")
    public ResponseEntity<List<RevisiumRequest>> listAllByService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId
    ) {
        List<RevisiumRequest> revisiumRequests = revisiumRequestRepository.findByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultAndCreatedAfter(
                accountId,
                revisiumRequestServiceId,
                true,
                LocalDateTime.now().minusYears(1L)
        );

        return new ResponseEntity<>(revisiumRequests, HttpStatus.OK);
    }

    //Один реквест в Ревизиум
    @GetMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}/requests/{revisiumRequestId}")
    public ResponseEntity<RevisiumRequest> getOneRequest(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequest.class) @PathVariable(value = "revisiumRequestId") String revisiumRequestId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId
    ) {
        RevisiumRequest revisiumRequest = revisiumRequestRepository.findByPersonalAccountIdAndRevisiumRequestServiceIdAndId(
                accountId,
                revisiumRequestServiceId,
                revisiumRequestId
        );

        return new ResponseEntity<>(revisiumRequest, HttpStatus.OK);
    }

    //Один ПОСЛЕДНИЙ УСПЕШНЫЙ реквест в Ревизиум
    @GetMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}/requests/last")
    public ResponseEntity<RevisiumRequest> getOneLastSuccessRequest(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId
    ) {
        RevisiumRequest revisiumRequest = revisiumRequestRepository
                .findFirstByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultOrderByCreatedDesc(
                        accountId,
                        revisiumRequestServiceId,
                        true
        );

        return new ResponseEntity<>(revisiumRequest, HttpStatus.OK);
    }

    //Продление услуги вручную
    @PostMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}/prolong")
    public ResponseEntity<RevisiumRequestService> prolong(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId,
            SecurityContextHolderAwareRequestWrapper request
    ) {

        PersonalAccount account = personalAccountManager.findOne(accountId);

        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestServiceId);

        if (revisiumRequestService == null) {
            throw new ParameterValidationException("RevisiumRequestService с ID: " + revisiumRequestServiceId + "не найден");
        }

        PaymentService paymentService = accountServiceHelper.getRevisiumPaymentService();

        accountHelper.checkBalance(account, paymentService);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService).setComment(revisiumRequestService.getSiteUrl()).build();
        accountHelper.charge(account, chargeMessage);

        accountHelper.saveHistory(account, "Произведен заказ продления услуги " + paymentService.getName(), request);

        accountServiceHelper.prolongAccountServiceExpiration(account, revisiumRequestService.getAccountServiceId(), 1L);

        revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestServiceId);

        return new ResponseEntity<>(revisiumRequestService, HttpStatus.OK);
    }



    //Заказ услуги ревизиума
    @PostMapping("/{accountId}/revisium/services")
    public ResponseEntity<RevisiumRequestService> request(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody @Valid RevisiumRequestBody revisiumRequestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {

        PersonalAccount account = personalAccountManager.findOne(accountId);

        if (account == null) {
            throw new ParameterValidationException("Аккаунт не найден");
        }

        String siteUrl = revisiumRequestBody.getSiteUrl();
        URL url;

        try {
            url = new URL(revisiumRequestBody.getSiteUrl());
        } catch (MalformedURLException e) {
            throw new ParameterValidationException("Введённый адрес сайта некорректен");
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int code = connection.getResponseCode();

            if (code == 301 || code == 302) {
                throw new ParameterValidationException("Обнаружен редирект на " + connection.getHeaderField("Location"));
            }

            if (code < 200 || code >= 300) {
                throw new ParameterValidationException("Некорректный ответ от сайта");
            }

        } catch (IOException e) {
            throw new ParameterValidationException("Введённый адрес сайта недоступен");
        }

        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findByPersonalAccountIdAndSiteUrl(account.getId(), siteUrl);

        if (revisiumRequestService != null) {
            throw new ParameterValidationException("Данный сайт уже добавлен на проверку");
        }

        //Услуга ревизиума
        PaymentService paymentService = accountServiceHelper.getRevisiumPaymentService();

        //Проверяем баланс аккаунта + добавляем услугу на аккаунт + добавляем до какого действует услуга
        accountHelper.checkBalanceWithoutBonus(account, paymentService);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService).setComment(siteUrl).build();

        //Списываем
        accountHelper.charge(account, chargeMessage);

        //Добавляем услугу
        AccountService accountService = accountServiceHelper.addAccountService(account, paymentService.getId());

        accountHelper.saveHistory(account, "Произведен заказ услуги " + paymentService.getName(), request);

        //Дата окончания действия услуги
        accountServiceHelper.prolongAccountServiceExpiration(account, accountService.getId(), 1L);

        //Ревизиум сервис
        revisiumRequestService = new RevisiumRequestService();
        revisiumRequestService.setPersonalAccountId(account.getId());
        revisiumRequestService.setAccountServiceId(accountService.getId());
        revisiumRequestService.setCreated(LocalDateTime.now());
        revisiumRequestService.setSiteUrl(siteUrl);
        revisiumRequestService.setAccountService(accountService);
        revisiumRequestServiceRepository.save(revisiumRequestService);

        accountServiceHelper.revisiumCheckRequest(account, revisiumRequestService);

        revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestService.getId());

        return new ResponseEntity<>(revisiumRequestService, HttpStatus.CREATED);
    }
}