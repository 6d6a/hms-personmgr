package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

@Data
public class LostClientInfo {
    private final PersonalAccount account;
    private Plan plan;
    private AccountOwner owner;
    private BigDecimal overallPaymentAmount = BigDecimal.ZERO;
    private Collection<Domain> domains = new ArrayList<>();
    private Abonement abonement;
}
