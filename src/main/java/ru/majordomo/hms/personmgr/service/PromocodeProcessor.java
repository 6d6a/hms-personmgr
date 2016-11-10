package ru.majordomo.hms.personmgr.service;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.ImportConstants.getPartnerPromocodeActionId;
import static ru.majordomo.hms.personmgr.common.StringConstants.BONUS_PAYMENT_TYPE_ID;

@Service
public class PromocodeProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeProcessor.class);

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private AccountPromocodeRepository accountPromocodeRepository;

    @Autowired
    private FinFeignClient finFeignClient;

    public void processPromocode(PersonalAccount account, String promocodeString) {
        Promocode promocode = promocodeRepository.findByCodeAndActive(promocodeString, true);

        if (promocode == null) {
            logger.info("Not found promocode instance with code: " + promocodeString);
            return;
        }

        AccountPromocode accountPromocode;

        switch (promocode.getType()) {
            case PARTNER:
                logger.info("Found PARTNER promocode instance with code: " + promocodeString);

                AccountPromocode ownerAccountPromocode = accountPromocodeRepository.findByPromocodeIdAndOwnedByAccount(promocode.getId(), true);

                //Если пытается использовать свой промокод
                if (account.getId().equals(ownerAccountPromocode.getPersonalAccountId())) {
                    logger.info("Client trying to use his own code: " + promocodeString);

                    return;
                }

                accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(false);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setPromocodeId(promocode.getId());

                accountPromocodeRepository.save(accountPromocode);

                processPartnerPromocodeActions(account, accountPromocode);

                break;
            case BONUS:
                logger.info("Found BONUS promocode instance with code: " + promocodeString);

                //Если уже кто-то такой имеет промокод
                accountPromocode = accountPromocodeRepository.findOneByPromocodeId(promocode.getId());
                if (accountPromocode != null) {
                    logger.info("Client trying to use already used code: " + promocodeString);

                    return;
                }

                accountPromocode = new AccountPromocode();
                accountPromocode.setOwnedByAccount(true);
                accountPromocode.setPersonalAccountId(account.getId());
                accountPromocode.setPromocodeId(promocode.getId());

                accountPromocodeRepository.save(accountPromocode);

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
        promocode.setActionIds(Collections.singletonList(getPartnerPromocodeActionId()));

        promocodeRepository.save(promocode);

        AccountPromocode accountPromocode = new AccountPromocode();
        accountPromocode.setPromocodeId(promocode.getId());
        accountPromocode.setOwnedByAccount(true);
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
        logger.info("Processing promocode actions for account: " + account.getName() + " for code: " + accountPromocode.getPromocode().getCode());

        for (PromocodeAction action : promocodeActions) {
            switch (action.getActionType()) {
                case BALANCE_FILL:
                    logger.info("Processing promocode BALANCE_FILL codeAction: " + action.toString());

                    Map<String, Object> payment = new HashMap<>();
                    payment.put("accountId", account.getName());
                    payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
                    payment.put("amount", new BigDecimal(action.getProperties().get("amount")));
                    payment.put("documentNumber", "N/A");
                    payment.put("message", "Бонусный платеж при использовании промокода " + accountPromocode.getPromocode().getCode());

                    try {
                        payment = finFeignClient.addPayment(payment);
                        logger.info("Processed promocode addPayment: " + payment.toString());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    }
}
