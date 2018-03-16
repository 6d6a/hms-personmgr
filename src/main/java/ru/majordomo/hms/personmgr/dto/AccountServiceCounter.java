package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccountServiceCounter extends ResourceCounter{
    private int quantity;
}
