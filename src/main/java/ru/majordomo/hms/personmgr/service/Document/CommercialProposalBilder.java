package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class CommercialProposalBilder extends DocumentBuilderImpl {

    public CommercialProposalBilder(boolean withoutStamp){
        setWithoutStamp(withoutStamp);
    }

    @Override
    public void buildReplaceParameters() {}

    @Override
    public void buildTemplate() {
        String resourceWithStamp = "/contract/commercial_proposal.pdf";
        String resourceWithoutStamp = "/contract/commercial_proposal_without_stamp.pdf";
        InputStream inputStream = this.getClass().getResourceAsStream(isWithoutStamp() ? resourceWithoutStamp : resourceWithStamp);
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
