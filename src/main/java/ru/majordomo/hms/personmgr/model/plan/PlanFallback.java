package ru.majordomo.hms.personmgr.model.plan;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanFallback extends BaseModel {
    @Indexed(unique = true)
    @NotNull
    @ObjectId(Plan.class)
    private String planId;
    @Indexed
    @NotNull
    @ObjectId(Plan.class)
    private String fallbackPlanId;

    @ObjectId(Plan.class)
    private String withAbonementFallbackPlanId;
}
