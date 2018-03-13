package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RevisiumAccountNotice extends AccountNotice {

    private String revisiumRequestId;
    private String revisiumRequestServiceId;
}
