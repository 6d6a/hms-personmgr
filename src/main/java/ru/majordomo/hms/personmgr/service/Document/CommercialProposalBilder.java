package ru.majordomo.hms.personmgr.service.Document;

import java.io.File;
import java.io.InputStream;

import static ru.majordomo.hms.personmgr.common.FileUtils.saveInputStreamToFile;

public class CommercialProposalBilder implements DocumentBuilder {

    private final static String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";
    private File file;
    private final static String fileName = "commercial_proposal.pdf";
    private final static String COMMERCIAL_PROPOSAL_RESOURCE_PATH = "/contract/" + fileName;

    @Override
    public void buildReplaceParameters() {

    }

    @Override
    public void buildTemplate() {
        String filePath = temporaryFilePath + fileName;

        File file = new File(filePath);

        if (file.exists()) {
            this.file = file;
        } else {
            InputStream inputStream = this.getClass()
                    .getResourceAsStream(COMMERCIAL_PROPOSAL_RESOURCE_PATH);

            try {
                saveInputStreamToFile(inputStream, filePath);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
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

    @Override
    public File getDocument() {
        return this.file;
    }
}
