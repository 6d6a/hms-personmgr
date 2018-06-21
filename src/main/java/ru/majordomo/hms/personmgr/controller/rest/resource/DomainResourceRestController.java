package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
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
import java.net.IDN;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@RestController
@RequestMapping("/{accountId}/domain")
@Validated
public class DomainResourceRestController extends CommonRestController {
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

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
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
        domainName = IDN.toUnicode(domainName);
        message.addParam("name", domainName);

        String parentDomainId = (String) message.getParam("parentDomainId");
        if (parentDomainId != null && !parentDomainId.equals("")) {
            Domain parentDomain = rcUserFeignClient.getDomain(accountId, parentDomainId);

            domainName = domainName.substring(domainName.length() - 1).equals(".") ?
                    domainName + parentDomain.getName() : domainName + "." + parentDomain.getName();
        }

        domainService.checkBlacklist(domainName, accountId);

        DomainTld domainTld = domainTldService.findActiveDomainTldByDomainName(domainName);

        if (isRegistration) {

            //Проверить домен на премиальность
            AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domainName);
            if (!availabilityInfo.getFree()) {
                throw new ResourceNotFoundException("Домен: " + domainName + " по данным whois занят.");
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
                accountHelper.checkBalanceWithoutBonus(account, paymentService);

                ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                        .excludeBonusPaymentType()
                        .build();

                SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);
                String documentNumber = (String) blockResult.getParam("documentNumber");
                message.addParam("documentNumber", documentNumber);
            }
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_CREATE, BusinessActionType.DOMAIN_CREATE_RC, message);

        String actionText = isRegistration ?
                (isFreeDomain ?
                        "бесплатную регистрацию (actionPromotion Id: " + message.getParam("freeDomainPromotionId") + " )" :
                        (isDiscountedDomain ?
                                "регистрацию со скидкой (actionPromotion Id: " + message.getParam("domainDiscountPromotionId") + " )" :
                                "регистрацию")) :
                "добавление";
        history.save(accountId, "Поступила заявка на " + actionText +" домена (имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
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

            BigDecimal pricePremium = domainRegistrarFeignClient.getRenewPremiumPrice(domain.getName());
            if (pricePremium != null && (pricePremium.compareTo(BigDecimal.ZERO) > 0)) {
                domainTld.getRenewService().setCost(pricePremium);
            }

            accountHelper.checkBalance(account, domainTld.getRenewService());
            accountHelper.checkBalanceWithoutBonus(account, domainTld.getRenewService());

            ChargeMessage chargeMessage = new ChargeMessage.Builder(domainTld.getRenewService())
                    .excludeBonusPaymentType()
                    .build();

            SimpleServiceMessage blockResult = accountHelper.block(account, chargeMessage);

            String documentNumber = (String) blockResult.getParam("documentNumber");
            message.addParam("documentNumber", documentNumber);
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_UPDATE, BusinessActionType.DOMAIN_UPDATE_RC, message);

        history.save(accountId, "Поступила заявка на " + (isRenew ? "продление" : "обновление") + " домена (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

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

        logger.debug("Deleting domain with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DOMAIN_DELETE, BusinessActionType.DOMAIN_DELETE_RC, message);

        history.save(accountId, "Поступила заявка на удаление домена (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
