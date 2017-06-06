package ru.majordomo.hms.personmgr.validation.groupSequenceProvider;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerBudgetCompanyChecks;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerChecks;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerCompanyChecks;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerIndividualChecks;


public class AccountOwnerGroupSequenceProvider implements DefaultGroupSequenceProvider<AccountOwner> {
    @Override
    public List<Class<?>> getValidationGroups(AccountOwner accountOwner) {
        List<Class<?>> sequence = new ArrayList<>();

        sequence.add(AccountOwner.class);

        sequence.add(AccountOwnerChecks.class);

        if(accountOwner != null && accountOwner.getType() != null){
            switch (accountOwner.getType()) {
                case INDIVIDUAL:
                    sequence.add(AccountOwnerIndividualChecks.class);
                    break;
                case COMPANY:
                    sequence.add(AccountOwnerCompanyChecks.class);
                    break;
                case BUDGET_COMPANY:
                    sequence.add(AccountOwnerBudgetCompanyChecks.class);
                    break;
            }
        }

        return sequence;
    }
}
