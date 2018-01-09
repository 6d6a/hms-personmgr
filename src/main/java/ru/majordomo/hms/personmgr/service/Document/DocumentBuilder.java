package ru.majordomo.hms.personmgr.service.Document;

import ru.majordomo.hms.personmgr.model.account.AccountDocument;

import java.io.File;


public interface DocumentBuilder {
    default File build(){
        checkAuthority();
        checkRequireParams();
        buildTemplate();
        buildReplaceParameters();
        replaceFields();
        convert();
        saveAccountDocument();
        return getDocument();
    }

    File buildFromAccountDocument(AccountDocument document);

    default void checkAuthority(){}

    void buildReplaceParameters();

    void buildTemplate();

    void replaceFields();

    void convert();

    default void checkRequireParams(){}

    void saveAccountDocument();

    File getDocument();
}