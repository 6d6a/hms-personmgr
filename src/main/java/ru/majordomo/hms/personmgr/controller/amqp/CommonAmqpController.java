package ru.majordomo.hms.personmgr.controller.amqp;

import java.net.MalformedURLException;
import java.net.URL;

public class CommonAmqpController {
    protected String getResourceIdByObjRef(String url) {
        try {
            URL processingUrl = new URL(url);
            String path = processingUrl.getPath();
            String[] pathParts = path.split("/");

            return pathParts[2];
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
