package ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.ftp;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class FtpAccountMessage extends GenericMessage {
    private ArrayList<HashMap<String, Object>> ftpAccounts;

    public ArrayList<HashMap<String, Object>> getFtpAccounts() {
        return ftpAccounts;
    }

    public HashMap<String, Object> getFtpAccount(Integer id) {
        HashMap<String, Object> searching = new HashMap<>();
        ftpAccounts.forEach((I) -> {
            if (I != null && I.get("id").equals(id)) {
                I.forEach(searching::put);
            }
        });
        return searching;
    }
}
