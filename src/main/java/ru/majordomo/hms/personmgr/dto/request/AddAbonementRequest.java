package ru.majordomo.hms.personmgr.dto.request;


import java.time.Period;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AddAbonementRequest {
    private String serviceId;
    @NotNull
    private Period period;
}
