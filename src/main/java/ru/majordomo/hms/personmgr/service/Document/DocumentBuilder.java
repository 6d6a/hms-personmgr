package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

public interface DocumentBuilder {
    default byte[] build(){
        prepare();
        checkAuthority();
        checkRequireParams();
        buildTemplate();
        buildReplaceParameters();
        replaceFields();
        convert();
        saveAccountDocument();
        return getFile();
    }

    default void check() throws ParameterValidationException {
        prepare();
        checkAuthority();
        checkRequireParams();
    }

    default void prepare(){}

    default byte[] buildFromAccountDocument(AccountDocument document){
        throw new NotImplementedException();
    }

    default byte[] buildPreview() {
        throw new NotImplementedException();
    }

    default void checkAuthority(){}

    default void buildReplaceParameters(){}

    void buildTemplate();

    void replaceFields();

    void convert();

    default void checkRequireParams(){}

    void saveAccountDocument();

    byte[] getFile();
}