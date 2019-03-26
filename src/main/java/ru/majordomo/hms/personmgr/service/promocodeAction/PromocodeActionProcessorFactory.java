package ru.majordomo.hms.personmgr.service.promocodeAction;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.PromocodeActionType;

@Service
public class PromocodeActionProcessorFactory {

    private final FreeDomainActionProcessor freeDomainActionProcessor;
    private final AbonementActionProcessor abonementActionProcessor;
    private final BalanceFillActionProcessor balanceFillActionProcessor;
    private final PaymentPercentBonusActionProcessor paymentPercentBonusProcessor;
    private final AddPromotionProcessor addPromotionProcessor;

    @Autowired
    public PromocodeActionProcessorFactory(
            FreeDomainActionProcessor freeDomainActionProcessor,
            AbonementActionProcessor abonementActionProcessor,
            BalanceFillActionProcessor balanceFillActionProcessor,
            PaymentPercentBonusActionProcessor paymentPercentBonusProcessor,
            AddPromotionProcessor addPromotionProcessor
    ) {
        this.freeDomainActionProcessor = freeDomainActionProcessor;
        this.abonementActionProcessor = abonementActionProcessor;
        this.balanceFillActionProcessor = balanceFillActionProcessor;
        this.paymentPercentBonusProcessor = paymentPercentBonusProcessor;
        this.addPromotionProcessor = addPromotionProcessor;
    }

    public PromocodeActionProcessor getProcessor(PromocodeActionType type) {
        switch (type) {
            case SERVICE_ABONEMENT:
                return abonementActionProcessor;

            case SERVICE_FREE_DOMAIN:
                return freeDomainActionProcessor;

            case BALANCE_FILL:
                return balanceFillActionProcessor;

            case PAYMENT_PERCENT_BONUS:
                return paymentPercentBonusProcessor;

            case ADD_PROMOTION:
                return addPromotionProcessor;

            case SERVICE_DOMAIN_DISCOUNT_RU_RF:
            default:
                throw new NotImplementedException("Нет обработчика акции с типом " + type.name());
        }
    }
}
