package ru.majordomo.hms.personmgr.Test;

import ru.majordomo.hms.personmgr.models.mailmanager.NewMailTask;
import ru.majordomo.hms.personmgr.service.MailManager;

public class MailManagerTest {

    public static void main(String[] args) {

        NewMailTask mailTask = new NewMailTask();
        mailTask.setApi_name("mj_scheduled_works_18_03_2016").setEmail("zaborshikov@majordomo.ru").addParametr("client_id", "12345").setPriority(10);
        System.out.println(mailTask);

        MailManager mailManager = new MailManager();
        System.out.println(mailManager.createTask(mailTask));
    }

}
