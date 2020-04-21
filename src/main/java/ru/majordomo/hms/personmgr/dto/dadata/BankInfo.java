package ru.majordomo.hms.personmgr.dto.dadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankInfo {
    private String bic;
    private String name;
    private String correspondentAccount;
}
