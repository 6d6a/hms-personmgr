package ru.majordomo.hms.personmgr.dto.dadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OwnerInfo {
    private String inn = "";
    private String kpp = "";
    private String okpo = "";
    private String ogrn = "";
    private String okved = "";
    private String addressFull = "";
    private String type = "";
    private String postal = "";
    private String city = "";
    private String addressShort = "";
    private String fullName = "";
    private String orgType = "";
    private String manager = "";
    private String firstName = "";
    private String lastName = "";
    private String middleName = "";
    private String mgrFirstName = "";
    private String mgrLastName = "";
    private String mgrMiddleName = "";
}
