package ru.majordomo.hms.personmgr.model.cerb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class CerbTicket extends BaseModel {
    @NotBlank
    private String violation;
    @NotBlank
    private String ticketMessage;
}