package ru.majordomo.hms.personmgr.model.plan;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectIdList;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.majordomo.hms.personmgr.common.ResourceType.*;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class ServicePlan extends BaseModel {
    @NotBlank
    private String name;

    @NotNull
    private Feature feature;

    @Indexed
    private boolean active;

    /** PaymentService.id */
    @NotBlank
    @Indexed
    private String serviceId;

    @NotNull
    @Indexed
    private boolean abonementOnly;

    /** услуга может быть подключена только для некоторых тарифных планов, проверка по полю Plan.getAllowedFeature */
    private boolean forSomePlan = false;

    @ObjectIdList(value = Abonement.class)
    private List<String> abonementIds = new ArrayList<>();

    @Transient
    private PaymentService service;

    @Transient
    private List<Abonement> abonements = new ArrayList<>();

    public String getNotInternalAbonementId() {
        Abonement abonement = getNotInternalAbonement();
        return abonement != null ? abonement.getId() : null;
    }

    public Abonement getNotInternalAbonement() {
        for (Abonement abonement : this.getAbonements()) {
            if (!abonement.isInternal()) {
                return abonement;
            }
        }
        return null;
    }

    @Nullable
    public Abonement getAbonementById(String abonementId) {
        return getAbonements().stream().filter(abonement -> abonement.getId().equals(abonementId)).findFirst().orElse(null);
    }
}
