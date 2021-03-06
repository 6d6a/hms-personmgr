package ru.majordomo.hms.personmgr.controller.rest;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.revisium.RevisiumRequestBody;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestServiceRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.Revisium.RevisiumRequestProcessor;
import ru.majordomo.hms.personmgr.service.ServiceAbonementService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.Valid;
import java.net.IDN;
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
    private final RevisiumRequestProcessor revisiumRequestProcessor;
    private final ServiceAbonementService serviceAbonementService;
    private final ServicePlanRepository servicePlanRepository;
    private final AccountHelper accountHelper;

    private final String[] SCHEMES = {"http","https"};

    @Autowired
    public RevisiumRequestRestController(
            RevisiumRequestRepository revisiumRequestRepository,
            PersonalAccountManager personalAccountManager,
            RevisiumRequestProcessor revisiumRequestProcessor,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository,
            ServiceAbonementService serviceAbonementService,
            ServicePlanRepository servicePlanRepository,
            AccountHelper accountHelper
    ) {
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.personalAccountManager = personalAccountManager;
        this.revisiumRequestProcessor = revisiumRequestProcessor;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
        this.serviceAbonementService = serviceAbonementService;
        this.servicePlanRepository = servicePlanRepository;
        this.accountHelper = accountHelper;
    }

    //???????????? ???????? ?????????? ??????????????????
    @GetMapping("/{accountId}/revisium/services")
    public ResponseEntity<List<RevisiumRequestService>> listAllServices(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<RevisiumRequestService> revisiumRequestServices = revisiumRequestServiceRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(revisiumRequestServices, HttpStatus.OK);
    }

    //???????? ???????????? ??????????????????
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

    //?????? ???????????????? ???????????????? ?? ???????????????? ???? ?????????????????? ??????
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

    //?????? ???????????????? ???????????????? ?? ???????????????? ???? ???????????? ?????????? ???? ?????????????????? ??????
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

    //???????? ?????????????? ?? ????????????????
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

    //???????? ?????????????????? ???????????????? ?????????????? ?? ????????????????
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

    //?????????????????? ???????????? ??????????????
    @PostMapping("/{accountId}/revisium/services/{revisiumRequestServiceId}/prolong")
    public ResponseEntity<RevisiumRequestService> prolong(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(RevisiumRequestService.class) @PathVariable(value = "revisiumRequestServiceId") String revisiumRequestServiceId,
            @RequestParam(value = "abonementId", required = false) String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {

        PersonalAccount account = personalAccountManager.findOne(accountId);

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.REVISIUM);

        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestServiceId);

        if (revisiumRequestService == null) {
            throw new ParameterValidationException("RevisiumRequestService ?? ID: " + revisiumRequestServiceId + "???? ????????????");
        }

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.REVISIUM, true);

        AccountServiceAbonement accountServiceAbonement;

        if (revisiumRequestService.getAccountServiceAbonement() != null) {
            serviceAbonementService.prolongAbonement(
                    account,
                    revisiumRequestService.getAccountServiceAbonement(),
                    abonementId
            );
        } else {
            accountServiceAbonement = serviceAbonementService.addAbonement(
                    account,
                    abonementId != null ? abonementId : plan.getNotInternalAbonementId(),
                    Feature.REVISIUM,
                    true
            );

            revisiumRequestService.setAccountServiceAbonementId(accountServiceAbonement.getId());
            revisiumRequestService.setAccountServiceAbonement(accountServiceAbonement);
            revisiumRequestServiceRepository.save(revisiumRequestService);
        }

        revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestServiceId);

        history.save(account, "???????????????????? ?????????? ?????????????????? ???????????? ????????????-???????????? ???? ???????????? ?? ??????????", request);

        return new ResponseEntity<>(revisiumRequestService, HttpStatus.OK);
    }



    //?????????? ???????????? ??????????????????
    @PostMapping("/{accountId}/revisium/services")
    public ResponseEntity<RevisiumRequestService> request(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody @Valid RevisiumRequestBody revisiumRequestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {

        PersonalAccount account = personalAccountManager.findOne(accountId);

        if (account == null) {
            throw new ParameterValidationException("?????????????? ???? ????????????");
        }

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.REVISIUM);

        URL url;

        try {
            url = new URL(revisiumRequestBody.getSiteUrl());
            url = new URL(url.getProtocol(), IDN.toASCII(url.getHost()),"");

            UrlValidator urlValidator = new UrlValidator(SCHEMES);
            if (!urlValidator.isValid(url.toString())) {
                throw new ParameterValidationException("?????????????????? ?????????? ?????????? ??????????????????????");
            }

        } catch (MalformedURLException e) {
            throw new ParameterValidationException("?????????????????? ?????????? ?????????? ??????????????????????");
        }

        final String siteUrl = url.toString();

//        try {
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
//            connection.connect();
//            connection.setConnectTimeout(5000);
//            connection.setReadTimeout(5000);
//
//            int code = connection.getResponseCode();
//
//            if (code == 301 || code == 302) {
//                throw new ParameterValidationException("?????????????????? ???????????????? ???? " + connection.getHeaderField("Location"));
//            }
//
//            if (code < 200 || code >= 300) {
//                throw new ParameterValidationException("???????????????????????? ?????????? ???? ??????????");
//            }
//
//        } catch (IOException e) {
//            throw new ParameterValidationException("?????????????????? ?????????? ?????????? ????????????????????");
//        }

        RevisiumRequestService revisiumRequestService = revisiumRequestServiceRepository.findByPersonalAccountIdAndSiteUrl(account.getId(), siteUrl);

        if (revisiumRequestService != null) {
            throw new ParameterValidationException("???????????? ???????? ?????? ???????????????? ???? ????????????????");
        }

        String abonementId = revisiumRequestBody.getAbonementId();

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.REVISIUM, true);

        AccountServiceAbonement accountServiceAbonement = serviceAbonementService.addAbonement(
                account,
                abonementId != null ? abonementId : plan.getNotInternalAbonementId(),
                Feature.REVISIUM,
                true
        );

        history.save(account, "???????????????????? ?????????? ???????????? ????????????-???????????? ???? ???????????? ?? ?????????? ?????? " + siteUrl, request);

        //???????????????? ????????????
        revisiumRequestService = new RevisiumRequestService();
        revisiumRequestService.setPersonalAccountId(account.getId());
        revisiumRequestService.setAccountServiceAbonementId(accountServiceAbonement.getId());
        revisiumRequestService.setAccountServiceAbonement(accountServiceAbonement);
        revisiumRequestService.setCreated(LocalDateTime.now());
        revisiumRequestService.setSiteUrl(siteUrl);
        revisiumRequestServiceRepository.save(revisiumRequestService);

        revisiumRequestProcessor.process(account, revisiumRequestService);

        revisiumRequestService = revisiumRequestServiceRepository
                .findByPersonalAccountIdAndId(accountId, revisiumRequestService.getId());

        return new ResponseEntity<>(revisiumRequestService, HttpStatus.CREATED);
    }
}