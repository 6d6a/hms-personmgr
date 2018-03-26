package ru.majordomo.hms.personmgr.event.bitrixLicenseOrder.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

@Component
public class BitrixLicenseOrderMongoEventListener extends AbstractMongoEventListener<BitrixLicenseOrder> {
    private final MongoOperations mongoOperations;

    @Autowired
    public BitrixLicenseOrderMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<BitrixLicenseOrder> event) {
        super.onAfterConvert(event);
        BitrixLicenseOrder order = event.getSource();

        PersonalAccount account = mongoOperations.findById(order.getPersonalAccountId(), PersonalAccount.class);

        if (account != null) {
            order.setPersonalAccountName(account.getName());
        }

        PaymentService paymentService = mongoOperations.findById(order.getServiceId(), PaymentService.class);

        if (paymentService != null) {
            order.setServiceName(paymentService.getName());
        }

        if (order.getPreviousOrderId() != null && !order.getPreviousOrderId().isEmpty()) {
            BitrixLicenseOrder previousOrder = mongoOperations.findById(
                            order.getPreviousOrderId(), BitrixLicenseOrder.class);
            order.setPreviousOrder(previousOrder);
        }
    }
}
