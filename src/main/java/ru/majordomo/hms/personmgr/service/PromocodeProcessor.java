package ru.majordomo.hms.personmgr.service;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.UnknownPromocode;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.*;

import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PAYMENT_TYPE_ID;

@Service
public class PromocodeProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeProcessor.class);

    private final PromocodeRepository promocodeRepository;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final FinFeignClient finFeignClient;
    private final AbonementService abonementService;
    private final PlanRepository planRepository;
    private final AbonementRepository abonementRepository;
    private final AccountPromotionRepository accountPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;
    private final UnknownPromocodeRepository unknownPromocodeRepository;
    protected final ApplicationEventPublisher publisher;

    @Autowired
    public PromocodeProcessor(
            PromocodeRepository promocodeRepository,
            AccountPromocodeRepository accountPromocodeRepository,
            FinFeignClient finFeignClient,
            AbonementService abonementService,
            PlanRepository planRepository,
            AbonementRepository abonementRepository,
            AccountPromotionRepository accountPromotionRepository,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper,
            UnknownPromocodeRepository unknownPromocodeRepository,
            ApplicationEventPublisher publisher
    ) {
        this.promocodeRepository = promocodeRepository;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.finFeignClient = finFeignClient;
        this.abonementService = abonementService;
        this.planRepository = planRepository;
        this.abonementRepository = abonementRepository;
        this.accountPromotionRepository = accountPromotionRepository;
        this.promotionRepository = promotionRepository;
        this.accountHelper = accountHelper;
        this.unknownPromocodeRepository = unknownPromocodeRepository;
        this.publisher = publisher;
    }

    public void processPromocode(PersonalAccount account, String promocodeString) {
        Promocode promocode = promocodeRepository.findByCodeAndActive(promocodeString, true);

        if (promocode == null) {
            if (!promocodeString.equals("")) {
                logger.debug("Not found promocode instance with code: " + promocodeString);
                // Записываем в базу промокоды, которые не были найдены
                UnknownPromocode unknownPromocode = new UnknownPromocode();
                unknownPromocode.setCode(promocodeString);
                unknownPromocode.setPersonalAccountId(account.getId());
                unknownPromocode.setCreatedDate(LocalDate.now());
                unknownPromocodeRepository.save(unknownPromocode);
            }
            return;
        }

        AccountPromocode accountPromocode;

        Map<String, String> paramsHistory;

        switch (promocode.getType()) {
            case PARTNER:
                logger.debug("Found PARTNER promocode instance with code: " + promocodeString);

                AccountPromocode ownerAccountPromocode = accountPromocodeRepository.findByPromocodeIdAndOwnedByAccount(promocode.getId(), true);

                //Если пытается использовать свой промокод
                if (account.getId().equals(ownerAccountPromocode.getPersonalAccountId())) {
                    logger.debug("Client trying to use his own code: " + promocodeString);

                    return;
                }

                accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(false);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setOwnerPersonalAccountId(ownerAccountPromocode.getPersonalAccountId());
                accountPromocode.setPromocodeId(promocode.getId());
                accountPromocode.setPromocode(promocode);

                accountPromocodeRepository.save(accountPromocode);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Клиент зарегистрирован с использованием партнресвкого промокода '" + promocodeString + "'");
                paramsHistory.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.PromocodeProcessor.processPromocode");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));

                processPartnerPromocodeActions(account, accountPromocode);

                break;
            case BONUS:
                logger.debug("Found BONUS promocode instance with code: " + promocodeString);

                //Если уже кто-то такой имеет промокод
                accountPromocode = accountPromocodeRepository.findOneByPromocodeId(promocode.getId());
                if (accountPromocode != null) {
                    logger.debug("Client trying to use already used code: " + promocodeString);

                    return;
                }

                accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(true);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setOwnerPersonalAccountId(account.getId());
                accountPromocode.setPromocodeId(promocode.getId());
                accountPromocode.setPromocode(promocode);

                accountPromocodeRepository.save(accountPromocode);

                //Save history
                paramsHistory = new HashMap<>();
                paramsHistory.put(HISTORY_MESSAGE_KEY, "Клиент зарегистрирован с использованием бонусного промокода '" + promocodeString + "'");
                paramsHistory.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.PromocodeProcessor.processPromocode");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));

                processBonusPromocodeActions(account, accountPromocode);

                break;
        }
    }

    public void generatePartnerPromocode(PersonalAccount account) {
        Promocode promocode = new Promocode();
        String code = null;

        while (code == null) {
            code = generateNewCode(PromocodeType.PARTNER);
        }

        promocode.setCode(code);
        promocode.setActive(true);
        promocode.setCreatedDate(LocalDate.now());
        promocode.setType(PromocodeType.PARTNER);
        promocode.setActionIds(Collections.singletonList(PARTNER_PROMOCODE_ACTION_ID));

        promocodeRepository.save(promocode);

        AccountPromocode accountPromocode = new AccountPromocode();
        accountPromocode.setPromocodeId(promocode.getId());
        accountPromocode.setOwnedByAccount(true);
        accountPromocode.setOwnerPersonalAccountId(account.getId());
        accountPromocode.setPersonalAccountId(account.getId());

        accountPromocodeRepository.save(accountPromocode);
    }

    private String generateNewCode(PromocodeType type) {
        String code = null;
        switch (type) {
            case PARTNER:
                code = RandomStringUtils.randomAlphabetic(3).toUpperCase() + RandomStringUtils.randomNumeric(6);

                break;
            case BONUS:
                break;
        }

        if (code != null) {
            return promocodeRepository.findByCode(code) == null ? code : null;
        }

        return null;
    }

    private void processPartnerPromocodeActions(PersonalAccount account, AccountPromocode accountPromocode) {
        List<PromocodeAction> promocodeActions = accountPromocode.getPromocode().getActions();
        logger.debug("Processing promocode actions for account: " + account.getName() + " for code: " + accountPromocode.getPromocode().getCode());

        for (PromocodeAction action : promocodeActions) {
            switch (action.getActionType()) {
                case BALANCE_FILL:
                    logger.debug("Processing promocode BALANCE_FILL codeAction: " + action.toString());

                    Map<String, Object> payment = new HashMap<>();
                    payment.put("accountId", account.getName());
                    payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
                    payment.put("amount", new BigDecimal((String) action.getProperties().get("amount")));
                    payment.put("documentNumber", account.getName() + "_" + accountPromocode.getPromocode().getCode());
                    payment.put("message", "Бонусный платеж при использовании промокода " + accountPromocode.getPromocode().getCode());

                    try {
                        String responseMessage = finFeignClient.addPayment(payment);
                        logger.debug("Processed promocode addPayment: " + responseMessage);

                    } catch (Exception e) {
                        logger.error("Exception in ru.majordomo.hms.personmgr.service.PromocodeProcessor.processPartnerPromocodeActions " + e.getMessage());
                        e.printStackTrace();
                    }

                    //Save history
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Зачислен бонусный платеж при использовании промокода " + accountPromocode.getPromocode().getCode());
                    params.put(OPERATOR_KEY, "service");

                    publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                    break;
            }
        }
    }

    private void processBonusPromocodeActions(PersonalAccount account, AccountPromocode accountPromocode) {
        List<PromocodeAction> promocodeActions = accountPromocode.getPromocode().getActions();
        logger.debug("Processing promocode actions for account: " + account.getName() + " for code: " + accountPromocode.getPromocode().getCode());

        for (PromocodeAction action : promocodeActions) {
            switch (action.getActionType()) {
                case SERVICE_ABONEMENT:
                    logger.debug("Processing promocode SERVICE_ABONEMENT codeAction: " + action.toString());

                    Plan plan = planRepository.findOne(account.getPlanId());
                    // Проверка на то что промокод соответствует тарифному плану
                    if (action.getProperties().get("serviceId").equals(plan.getServiceId())) {

                        List<String> abonementIds = plan.getAbonementIds();

                        String bonusAbonementId = null;

                        // Ищем соответствующий abonementId по периоду и плану
                        for (String abonementId : abonementIds) {
                            if ( (abonementRepository.findOne(abonementId).getPeriod()).equals(action.getProperties().get("period")) ) {
                                bonusAbonementId = abonementId;
                                break;
                            }
                        }

                        if (bonusAbonementId != null) {
                            abonementService.addAbonement(account, bonusAbonementId, false, true, false);

                            //Save history
                            Map<String, String> params = new HashMap<>();
                            params.put(HISTORY_MESSAGE_KEY, "Добавлен абонемент при использовании промокода " + accountPromocode.getPromocode().getCode());
                            params.put(OPERATOR_KEY, "service");

                            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                        } else {
                            throw new ParameterValidationException("abonementId with period: " + action.getProperties().get("period") + " not found for planName: " + plan.getName());
                        }
                    } else {
                        throw new ParameterValidationException("Wrong promocode action '" + action.toString() + "' for planId: " + account.getPlanId());
                    }
                    break;
                case SERVICE_FREE_DOMAIN:
                    logger.debug("Processing promocode SERVICE_FREE_DOMAIN codeAction: " + action.toString());
                    Promotion promotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
                    List<AccountPromotion> accountPromotions = accountPromotionRepository.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
                    if (accountPromotions == null || accountPromotions.isEmpty()) {
                        accountHelper.giveGift(account, promotion);

                        //Save history
                        Map<String, String> params = new HashMap<>();
                        params.put(HISTORY_MESSAGE_KEY, "Добавлен бонус на регистрацию бесплатного домена RU, РФ при использовании промокода " + accountPromocode.getPromocode().getCode());
                        params.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                    }
                    break;
            }
        }
    }
}
