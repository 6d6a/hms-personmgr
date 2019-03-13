package ru.majordomo.hms.personmgr.model.order.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class Country extends BaseModel {
    @NotBlank
    private String value;
    @NotBlank
    private String label;
}
