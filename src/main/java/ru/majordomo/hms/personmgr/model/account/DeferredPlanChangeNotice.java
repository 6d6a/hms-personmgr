package ru.majordomo.hms.personmgr.model.account;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Document(collection = AccountNotice.COLLECTION_NAME)
public class DeferredPlanChangeNotice extends AccountNotice {
     @NotNull
    private LocalDate willBeChangedAfter;

    private boolean wasChanged = false;

    public DeferredPlanChangeNotice() {
        super(AccountNoticeType.DEFERRED_PLAN_CHANGE);
    }
}
