package ru.majordomo.hms.personmgr.service.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentBuilderImpl implements DocumentBuilder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected byte[] file = new byte[1];

    protected boolean withoutStamp;

    protected void setWithoutStamp(boolean withoutStamp) {
        this.withoutStamp = withoutStamp;
    }

    protected boolean isWithoutStamp(){
        return this.withoutStamp;
    }

    protected void setFile(byte[] file){
        this.file = file;
    }

    @Override
    public byte[] getFile(){
        return this.file;
    }


}
