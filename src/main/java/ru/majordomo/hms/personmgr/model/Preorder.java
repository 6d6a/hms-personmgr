package ru.majordomo.hms.personmgr.model;

import lombok.*;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.annotation.Nullable;
import java.time.LocalDateTime;


/**
 * Одна предзаказанная услуга. У одного аккаунта их может быть много
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Preorder extends ModelBelongsToPersonalAccount implements Comparable<Preorder> {
    @NonNull
    private LocalDateTime created;
    @NonNull
    private Feature feature; // одно из значений model.plan.Feature. Нужно так как сложно определить это тарифный план или услуга и Feature по PaymentService услуги

    @Nullable
    private String chargeDocumentNumber; // Должно быть заполнено если предзаказанная услуга оплачена. Например 3804530

    @Nullable
    @ObjectId(AccountService.class)
    private String accountServiceId;
    @Nullable
    @ObjectId(AccountAbonement.class)
    private String accountAbonementId;  // если услуга добавлена, но прездаказ не удален, будет содержать ид услуги
    @Nullable
    @ObjectId(AccountServiceAbonement.class)
    private String AccountServiceAbonementId;

    /**
     * установить для абонемента, если это посуточные списания оставить в null
     */
    @Nullable
    @ObjectId(Abonement.class)
    private String abonementId;
    @Nullable
    @Transient
    private Abonement abonement;

    /**
     * установить для посуточных списаний и абонементов
     */
    @NonNull
    @ObjectId(PaymentService.class)
    private String paymentServiceId;
    @Transient
    private PaymentService paymentService;


    public void setAbonement(@NonNull Abonement abonement) {
        this.setAbonementId(abonement.getId());
        this.abonement = abonement;
    }

    public void setPaymentService(@NonNull PaymentService paymentService) {
        this.setPaymentServiceId(paymentService.getId());
        this.paymentService = paymentService;
    }

    public boolean isDaily() {
        return abonementId == null;
    }

    @Override
    public int compareTo(@Nullable Preorder preorder) {
        if (preorder == null ||
                (getFeature() == Feature.VIRTUAL_HOSTING_PLAN && preorder.getFeature() != Feature.VIRTUAL_HOSTING_PLAN)
        ) {
            return -1;
        } else if (getFeature() != Feature.VIRTUAL_HOSTING_PLAN && preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
            return 1;
        }
        return 0;
    }
}
