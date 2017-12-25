package ru.majordomo.hms.personmgr.service.Document;

import java.io.File;


public interface DocumentBuilder {
    default File build(){
        checkAuthority();
        buildTemplate();
        replaseFields();
        convert();
        return getDocument();
    }

    default void checkAuthority(){}

    void buildTemplate();

    void replaseFields();

    void convert();

    File getDocument();
}