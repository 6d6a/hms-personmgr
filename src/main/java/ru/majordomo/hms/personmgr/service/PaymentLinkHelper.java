package ru.majordomo.hms.personmgr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkRequest;
import ru.majordomo.hms.personmgr.dto.fin.PaymentLinkResponse;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.TokenManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.token.Token;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.AMOUNT_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PAYMENT_REDIRECT_PATH;

@Slf4j
@Service
public class PaymentLinkHelper {

    private final FinFeignClient finFeignClient;
    private final TokenManager tokenManager;
    private final PersonalAccountManager accountManager;
    private final String apiUrl;

    @Autowired
    public PaymentLinkHelper(
            FinFeignClient finFeignClient,
            TokenManager tokenManager,
            PersonalAccountManager accountManager,
            @Value("${hms.apiUrl}") String apiUrl) {
        this.finFeignClient = finFeignClient;
        this.tokenManager = tokenManager;
        this.accountManager = accountManager;
        this.apiUrl = apiUrl;
    }

    public PaymentLinkResponse generatePaymentLinkForMail(PersonalAccount account, PaymentLinkRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put(AMOUNT_KEY, request.getAmount());

        String tokenId = tokenManager.generateToken(account, TokenType.PAYMENT_REDIRECT, params);

        return new PaymentLinkResponse(apiUrl + PAYMENT_REDIRECT_PATH + "?token=" + tokenId);
    }

    public PaymentLinkResponse createPayment(String tokenId) {
        Token token = tokenManager.getToken(tokenId, TokenType.PAYMENT_REDIRECT);

        if (token == null) {
            throw new ParameterValidationException("?????????? ???? ????????????");
        }

        PersonalAccount account = accountManager.findOne(token.getPersonalAccountId());

        if (account == null) {
            throw new ParameterValidationException("?????????????? ???? ????????????.");
        }

        BigDecimal cost = Utils.getBigDecimalFromUnexpectedInput(token.getParam(AMOUNT_KEY));

        return finFeignClient.generatePaymentLink(
                account.getId(),
                new PaymentLinkRequest(cost)
        );
    }

    public String getPaymentLink(String tokenId) {
        try {
            return createPayment(tokenId).getPaymentLink();
        } catch (Exception e) {
            log.error("tokenId {} e {} message {}", tokenId, e.getClass(), e.getMessage());
            return "https://majordomo.ru/login?page=%2Fadd_money";
        }
    }
}
