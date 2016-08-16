package ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.ftp;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;

public class FtpAccountCountMessage extends GenericMessage {

    private Integer count;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

}
