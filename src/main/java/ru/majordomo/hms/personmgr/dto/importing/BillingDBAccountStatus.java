package ru.majordomo.hms.personmgr.dto.importing;

import lombok.Data;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.rc.staff.resources.Server;

import javax.annotation.Nullable;
import java.time.LocalDate;

@Data
public class BillingDBAccountStatus {
    private String accountId;
    private boolean onHms;
    private boolean allowImport;
    /**
     * rc-staff Server.id
     */
    private String hmsServerId;
    @Nullable
    private Server hmsServer;
    private LocalDate createDate;
    private int planOldId;
    /**
     * поле account.status в billingdb. Один из признаков неактивного аккаунта
     */
    private boolean status;
    @Nullable
    private Plan hmsPlan;
}
