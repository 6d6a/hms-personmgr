package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectIdList;

import static ru.majordomo.hms.personmgr.common.ResourceType.MAILBOX;
import static ru.majordomo.hms.personmgr.common.ResourceType.SSL_CERTIFICATE;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class Plan extends BaseModel {
    @NotNull
    private String name;

    @NotNull
    private String internalName;

    @NotNull
    @Indexed
    private String serviceId;

    @NotNull
    @Indexed
    private String oldId;

    @NotNull
    private AccountType accountType;

    @NotNull
    @Indexed
    private boolean abonementOnly;

    @Indexed
    private boolean active;

    @NotNull
    @Valid
    private PlanProperties planProperties;

    private Set<ResourceType> prohibitedResourceTypes = new HashSet<>();

    @ObjectIdList(value = Abonement.class)
    private List<String> abonementIds = new ArrayList<>();

    private String smsServiceId;

    @Transient
    private PaymentService service;

    @Transient
    private List<Abonement> abonements = new ArrayList<>();

    @Transient
    private PaymentService smsService;

    public Boolean isSslCertificateAllowed() {
        return !prohibitedResourceTypes.contains(SSL_CERTIFICATE);
    }

    public Boolean isMailboxAllowed() {
        return !prohibitedResourceTypes.contains(MAILBOX);
    }

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

    public Abonement getFree14DaysAbonement() {
        for (Abonement abonement : this.getAbonements()) {
            if (abonement.isInternal() && abonement.getPeriod().equals("P14D")) {
                return abonement;
            }
        }
        return null;
    }
}
