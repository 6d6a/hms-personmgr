package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class Abonement extends BaseModel {
    @NotNull
    private Feature type;
    @NotNull
    private String name;
    @NotNull
    private String period;
    @NotNull
    @ObjectId(PaymentService.class)
    private String serviceId;

    @NotNull
    @Indexed
    private boolean internal;

    @Transient
    private PaymentService service;
}
