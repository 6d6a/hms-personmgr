package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.DomainNotAvailableException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.RU_RF_DOMAINS;

@RestController
@RequestMapping("/{accountId}/domain")
@Validated
public class DomainResourceRestController extends CommonResourceRestController {
    private final DomainTldService domainTldService;
    private final AccountHelper accountHelper;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountPromotionManager accountPromotionManager;
    private final PromocodeActionRepository promocodeActionRepository;
    private final DomainService domainService;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;

    @Autowired
    public DomainResourceRestController(
            DomainTldService domainTldService,
            AccountHelper accountHelper,
            RcUserFeignClient rcUserFeignClient,
            AccountPromotionManager accountPromotionManager,
            PromocodeActionRepository promocodeActionRepository,
            DomainService domainService,
            DomainRegistrarFeignClient domainRegistrarFeignClient
    ) {
        this.domainTldService = domainTldService;
        this.accountHelper = accountHelper;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountPromotionManager = accountPromotionManager;
        this.promocodeActionRepository = promocodeActionRepository;
        this.domainService = domainService;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Добавление домена невозможно.");
        }

        logger.debug("Creating domain " + message.toString());

        boolean isRegistration = message.getParam("register") != null && (boolean) message.getParam("register");
        boolean isFreeDomain = false;
        boolean isDiscountedDomain = false;

        String domainName = (String) message.getParam("name");
        domainName = domainName.toLowerCase();
        message.addParam("name", domainName);

        try {
            domainService.checkBlacklist(domainName, accountId);
        } catch (DomainNotAvailableException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return this.createErrorResponse("Домен: " + domainName + " уже присутствует в системе и не может быть добавлен.");
        }

        DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domainName);

        if (isRegistration) {

            //Проверить домен на премиальность
            AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domainName);
            if (!availabilityInfo.getFree()) {
                return this.createErrorResponse("Домен: " + domainName + " по данным whois занят.");
            }

            List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountId(account.getId());
            for (AccountPromotion accountPromotion : accountPromotions) {
                Map<String, Boolean> map = accountPromotion.getActionsWithStatus();
                if (map.get(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID) != null && map.get(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID)) {

                    PromocodeAction promocodeAction = promocodeActionRepository.findOne(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID);
                    List<String> availableTlds = (List<String>) promocodeAction.getProperties().get("tlds");

                    if (availableTlds.contains(domainTld.getTld())) {
                        map.put(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID, false);
                        accountPromotion.setActionsWithStatus(map);
                        // Сохраняем с отметкой, что action использован
                        accountPromotionManager.deactivateAccountPromotionByIdAndActionId(
                                accountPromotion.getId(),
                                BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID
                        );
                        isFreeDomain = true;
                        message.addParam("freeDomainPromotionId", accountPromotion.getId());
                        break;
                    }
                }
            }

            PaymentService paymentService = domainTld.getRegistrationService();

            for (AccountPromotion accountPromotion : accountPromotions) {
                Map<String, Boolean> map = accountPromotion.getActionsWithStatus();
                if (map.get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID) != null && map.get(DOMAIN_DISCOUNT_RU_RF_ACTION_ID)) {
                    PromocodeAction promocodeAction = promocodeActionRepository.findOne(DOMAIN_DISCOUNT_RU_RF_ACTION_ID);
                    List<String> availableTlds = (List<String>) promocodeAction.getProperties().get("tlds");

                    if (availableTlds.contains(domainTld.getTld())) {
                        map.put(DOMAIN_DISCOUNT_RU_RF_ACTION_ID, false);
                        accountPromotion.setActionsWithStatus(map);
                        // Сохраняем с отметкой, что action использован
                        accountPromotionManager.deactivateAccountPromotionByIdAndActionId(
                                accountPromotion.getId(),
                                DOMAIN_DISCOUNT_RU_RF_ACTION_ID
                        );

                        // Устанавливает цену со скидкой
                        paymentService.setCost(BigDecimal.valueOf((Integer) promocodeAction.getProperties().get("cost")));

                        message.addParam("domainDiscountPromotionId", accountPromotion.getId());
                        isDiscountedDomain = true;
                        break;
                    }
                }
            }

            //Проверить домен на премиальность, если да - установить новую цену
            if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
                paymentService.setCost(availabilityInfo.getPremiumPrice());
                isFreeDomain = false;
                isDiscountedDomain = false;
            }

            if (!isFreeDomain) {
                accountHelper.checkBalance(account, paymentService);
                accountHelper.checkBalanceWithoutBonus(account, paymentService);
                SimpleServiceMessage blockResult = accountHelper.block(account, paymentService, true);
                String documentNumber = (String) blockResult.getParam("documentNumber");
                message.addParam("documentNumber", documentNumber);
            }
        }

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DOMAIN_CREATE, BusinessActionType.DOMAIN_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        String actionText = isRegistration ?
                (isFreeDomain ?
                        "бесплатную регистрацию" :
                        (isDiscountedDomain ?
                                "регистрацию со скидкой" :
                                "регистрацию")) :
                "добавление";
        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на " + actionText +" домена (имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating domain with id " + resourceId + " " + message.toString());

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление домена невозможно.");
        }

        boolean isRenew = message.getParam("renew") != null && (boolean) message.getParam("renew");

        if (isRenew) {
            Domain domain = rcUserFeignClient.getDomain(accountId, resourceId);

            DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

            AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domain.getName());
            if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
                domainTld.getRenewService().setCost(availabilityInfo.getPremiumPrice());
            }

            accountHelper.checkBalance(account, domainTld.getRenewService());
            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRenewService());

            SimpleServiceMessage blockResult = accountHelper.block(account, domainTld.getRenewService(), true);

            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DOMAIN_UPDATE, BusinessActionType.DOMAIN_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на " + (isRenew ? "продление" : "обновление") + " домена (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");
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

        logger.debug("Deleting domain with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DOMAIN_DELETE, BusinessActionType.DOMAIN_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на удаление домена (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return this.createSuccessResponse(businessAction);
    }
}
