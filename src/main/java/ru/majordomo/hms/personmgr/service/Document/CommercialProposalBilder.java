package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class CommercialProposalBilder extends DocumentBuilderImpl {

    @Override
    public void buildReplaceParameters() {}

    @Override
    public void buildTemplate() {
        InputStream inputStream = this.getClass().getResourceAsStream("/contract/commercial_proposal.pdf");
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
    public void replaceFields() {}

    @Override
    public void convert() {}

    @Override
    public void saveAccountDocument() {}
}
