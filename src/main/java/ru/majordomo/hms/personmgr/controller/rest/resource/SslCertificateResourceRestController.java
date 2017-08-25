package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.IDN;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@RequestMapping("/{accountId}/ssl-certificate")
@Validated
public class SslCertificateResourceRestController extends CommonResourceRestController {

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

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
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
                    Domain parentDomain = rcUserFeignClient.getDomain(accountId, domain.getId());

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

        ProcessingBusinessAction businessAction = process(BusinessOperationType.SSL_CERTIFICATE_CREATE, BusinessActionType.SSL_CERTIFICATE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на создание SSL-сертификата (имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String resourceId,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("resourceId", resourceId);
        message.setAccountId(accountId);

        logger.debug("Deleting sslcertificate with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.SSL_CERTIFICATE_DELETE, BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на удаление SSL-сертификата (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }
}
