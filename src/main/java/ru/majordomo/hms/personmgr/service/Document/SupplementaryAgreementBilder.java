package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.io.IOUtils;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;

import java.io.IOException;
import java.io.InputStream;

public class SupplementaryAgreementBilder extends DocumentBuilderImpl {

    private AccountOwner owner;

    public SupplementaryAgreementBilder(
            String personalAccountId,
            AccountOwnerManager accountOwnerManager
    ) {
        this.owner = accountOwnerManager.findOneByPersonalAccountId(personalAccountId);
    }

    @Override
    public void checkAuthority() {
        if (owner == null || !owner.getType().equals(AccountOwner.Type.BUDGET_COMPANY)) {
            throw new ParameterValidationException("Вы не можете заказать такой документ");
        }
    }

    @Override
    public void buildReplaceParameters() {
    }

    @Override
    public void buildTemplate() {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/contract/supplementary_agreement_budget_company.doc");

        try {
            setFile(
                    IOUtils.toByteArray(inputStream)
            );
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void replaceFields() {
    }

    @Override
    public void convert() {
    }

    @Override
    public void saveAccountDocument() {
    }
}
