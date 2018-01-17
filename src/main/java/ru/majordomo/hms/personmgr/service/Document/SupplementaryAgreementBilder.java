package ru.majordomo.hms.personmgr.service.Document;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;

import java.io.File;
import java.io.InputStream;

import static ru.majordomo.hms.personmgr.common.FileUtils.saveInputStreamToFile;

public class SupplementaryAgreementBilder implements DocumentBuilder {

    private AccountOwner owner;
    private final static String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";
    private File file;
    private final static String fileName = "supplementary_agreement_budget_company.doc";
    private final static String AGREEMENT_RESOURCE_PATH = "/contract/" + fileName;

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
    public void buildReplaceParameters() {}

    @Override
    public void buildTemplate() {
        String filePath = temporaryFilePath + fileName;

        File file = new File(filePath);

        if (file.exists()) {
            this.file = file;
        } else {
            InputStream inputStream = this.getClass()
                    .getResourceAsStream(AGREEMENT_RESOURCE_PATH);

            try {
                saveInputStreamToFile(inputStream, filePath);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void replaceFields() {}

    @Override
    public void convert() {}

    @Override
    public void saveAccountDocument() {}

    @Override
    public File getDocument() {
        return file;
    }
}
