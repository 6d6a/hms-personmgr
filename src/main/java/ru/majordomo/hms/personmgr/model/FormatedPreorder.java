package ru.majordomo.hms.personmgr.model;

import lombok.*;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * удобное для обработки на стороне frontend описание одной предзаказанной услуги
 */
@Data
@NoArgsConstructor
public class FormatedPreorder {
    @NonNull
    private String name; // Имя предзаказанной услуги которое можно показывать пользователю

    private BigDecimal cost;
    @NonNull
    private Feature feature;
    @NonNull
    private BigDecimal costWithoutDiscount; // цена без скидки. Нужна так как фронтэнд должен отобразить старую стоимость

    @NonNull
    private Period period;
    private boolean trial; // если абонемент пробный
    private boolean freePromo;
    private boolean daily;

    @Nullable
    private Plan plan; // Тарифный план или null если для доп услуг. Актуально только для VIRTUAL_HOSTING_PLAN
    @NonNull
    private String preorderId;  // id - предзаказа из коллекции preorder

    @NonNull
    private Preorder preorder;
}
