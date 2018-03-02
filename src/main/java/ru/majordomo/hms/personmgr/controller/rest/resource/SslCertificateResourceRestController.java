package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.IDN;
import java.net.UnknownHostException;

import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

@RestController
@RequestMapping("/{accountId}/ssl-certificate")
@Validated
public class SslCertificateResourceRestController extends CommonRestController {

    private final PlanRepository planRepository;
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public SslCertificateResourceRestController(
            PlanRepository planRepository,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.planRepository = planRepository;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating sslcertificate " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Заказ SSL-сертификата невозможен.");
        }

        Plan plan = planRepository.findOne(account.getPlanId());

        if (!plan.isSslCertificateAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане заказ SSL сертификатов недоступен");
        }

        String domainName = (String) message.getParam("name");

        Boolean canOrderSSL = false;
        Boolean hasAlienNS = false;

        try {
            Domain domain = rcUserFeignClient.findDomain(domainName);

            if (domain != null) {
                if (!accountId.equals(domain.getAccountId())) {
                    throw new ParameterValidationException("Домен не найден на вашем аккаунте");
                }
                if (domain.getParentDomainId() != null) {
                    Domain parentDomain = rcUserFeignClient.getDomain(accountId, domain.getParentDomainId());

                    if (parentDomain != null) {
                        if (!accountId.equals(parentDomain.getAccountId())) {
                            throw new ParameterValidationException("Основной домен для поддомена не найден на вашем аккаунте");
                        }
                        domainName = parentDomain.getName();
                    }
                }
            }
            //TODO не ясно для чего было так сделано (InternetDomainName.from(IDN.toASCII(domainName)).topPrivateDomain().toString())
            //но в новой гуаве оно перестало понимать наши домены третьего уровня типа blabla.org.ru (ищет по org.ru NS-ки)
            Lookup lookup = new Lookup(IDN.toASCII(domainName), Type.NS);
            lookup.setResolver(new SimpleResolver("8.8.8.8"));
            lookup.setCache(null);

            Record[] records = lookup.run();

            if (records != null) {
                for (Record record : records) {
                    NSRecord nsRecord = (NSRecord) record;
                    if (nsRecord.getTarget().equals(Name.fromString("ns.majordomo.ru.")) ||
                            nsRecord.getTarget().equals(Name.fromString("ns2.majordomo.ru.")) ||
                            nsRecord.getTarget().equals(Name.fromString("ns3.majordomo.ru.")) ) {
                        canOrderSSL = true;
                    } else {
                        hasAlienNS = true;
                    }
                }
            }
        } catch (UnknownHostException | TextParseException e) {
            logger.error("Ошибка при получении NS-записей: " + e.getMessage());
            e.printStackTrace();
        }

        if (!canOrderSSL || hasAlienNS) {
            throw new ParameterValidationException("Домен должен быть делегирован на наши DNS-серверы (ns.majordomo.ru, ns2.majordomo.ru и ns3.majordomo.ru)");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.SSL_CERTIFICATE_CREATE, BusinessActionType.SSL_CERTIFICATE_CREATE_RC, message);

        saveHistory(request, accountId, "Поступила заявка на создание SSL-сертификата (имя: " + message.getParam("name") + ")");

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable String resourceId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("resourceId", resourceId);
        message.setAccountId(accountId);

        logger.debug("Deleting sslcertificate with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.SSL_CERTIFICATE_DELETE, BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

        saveHistory(request, accountId, "Поступила заявка на удаление SSL-сертификата (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
