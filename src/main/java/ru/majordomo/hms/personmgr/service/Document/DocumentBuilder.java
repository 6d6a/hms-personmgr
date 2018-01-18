package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

import java.io.File;


public interface DocumentBuilder {
    default File build(){
        prepare();
        checkAuthority();
        checkRequireParams();
        buildTemplate();
        buildReplaceParameters();
        replaceFields();
        convert();
        saveAccountDocument();
        return getDocument();
    }

    default void prepare(){}

    default File buildFromAccountDocument(AccountDocument document){
        throw new NotImplementedException();
    }

    default void checkAuthority(){}

    default void buildReplaceParameters(){}

    void buildTemplate();

    void replaceFields();

    void convert();

    default void checkRequireParams(){}

    void saveAccountDocument();

    File getDocument();
}