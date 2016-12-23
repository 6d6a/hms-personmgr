package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.rc.user.resources.Person;

@Service
public class AccountHelper {
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public AccountHelper(RcUserFeignClient rcUserFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    public String getEmail(PersonalAccount account) {
        String clientEmails = "";

        Person person = null;
        if (account.getOwnerPersonId() != null) {
            person = rcUserFeignClient.getPerson(account.getId(), account.getOwnerPersonId());
        }

        if (person != null) {
            clientEmails = String.join(", ", person.getEmailAddresses());
        }

        return clientEmails;
    }
}
