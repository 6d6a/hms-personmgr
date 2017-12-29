package ru.majordomo.hms.personmgr.service.Document;

import java.io.File;


public interface DocumentBuilder {
    default File build(){
        checkAuthority();
        checkRequireParams();
        buildTemplate();
        replaceFields();
        convert();
        return getDocument();
    }

    default void checkAuthority(){}

    void buildTemplate();

    void replaceFields();

    void convert();

    default void checkRequireParams(){}

    File getDocument();
}