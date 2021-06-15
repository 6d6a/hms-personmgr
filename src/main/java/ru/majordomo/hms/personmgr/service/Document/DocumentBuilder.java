package ru.majordomo.hms.personmgr.service.Document;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.dto.request.DocumentPreviewRequest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

import javax.annotation.Nullable;

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

    /**
     * @param documentPreviewRequest
     * @return
     * @throws NotImplementedException
     */
    default byte[] buildPreview(DocumentPreviewRequest documentPreviewRequest) throws NotImplementedException {
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