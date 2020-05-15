package ru.majordomo.hms.personmgr.event.paymentService.listener;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.majordomo.hms.personmgr.common.Constants.ACTION_BITRIX_END_DATE;

@Component
public class PaymentServiceMongoEventListener extends AbstractMongoEventListener<PaymentService> {

    @Override
    public void onAfterConvert(AfterConvertEvent<PaymentService> event) {
        super.onAfterConvert(event);
        PaymentService paymentService = event.getSource();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime endDate = LocalDateTime.parse(ACTION_BITRIX_END_DATE, formatter);

        if (now.isBefore(endDate)) {
            if (paymentService.getOldId().equals("service_40")) {
                paymentService.setCost(new BigDecimal(4320L));
            }
            if (paymentService.getOldId().equals("service_41")) {
                paymentService.setCost(new BigDecimal(12720L));
            }
            if (paymentService.getOldId().equals("service_42")) {
                paymentService.setCost(new BigDecimal(28720L));
            }
            if (paymentService.getOldId().equals("service_44")) {
                paymentService.setCost(new BigDecimal(58320L));
            }
        }
    }
}

