package ru.majordomo.hms.personmgr.service.order;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.order.QBitrixLicenseOrder;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import java.util.List;

@Slf4j
public abstract class BitrixCommonOrderManager  extends OrderManager<BitrixLicenseOrder> {

    @Value("${mail_manager.bitrix_order_email}")
    protected String bitrixOrderEmail;

    protected final ApplicationEventPublisher publisher;
    protected final AccountHelper accountHelper;
    protected final PersonalAccountManager personalAccountManager;
    protected final PaymentServiceRepository paymentServiceRepository;
    protected final FinFeignClient finFeignClient;
    protected final AccountNotificationHelper accountNotificationHelper;

    public BitrixCommonOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient,
            AccountNotificationHelper accountNotificationHelper
    ){
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
    }

    @Override
    protected void onDecline(BitrixLicenseOrder order) {
        //Возвращаем блокированные средства
        try {
            finFeignClient.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in " + getClass() + ".onDecline #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish(BitrixLicenseOrder order) {
        //Переводим блокировку в реальное списание
        try {
            finFeignClient.chargeBlocked(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in " + getClass() + ".onFinish #1 " + e.getMessage());
        }
    }

    protected boolean isProlonged(BitrixLicenseOrder order){
        QBitrixLicenseOrder qOrder = QBitrixLicenseOrder.bitrixLicenseOrder;
        BooleanBuilder builder = new BooleanBuilder();

        Predicate predicate = builder
                .and(qOrder.previousOrderId.eq(order.getId()))
                .and(qOrder.type.eq(BitrixLicenseOrder.LicenseType.PROLONG))
                .and(qOrder.state.notIn(OrderState.DECLINED))
                .and(qOrder.personalAccountId.eq(order.getPersonalAccountId()));

        List<BitrixLicenseOrder> orders = accountOrderRepository.findAll(predicate);

        return orders != null && !orders.isEmpty();
    }

}
