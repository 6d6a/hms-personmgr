package ru.majordomo.hms.personmgr.dto.push;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.HashMap;
import java.util.Map;

public class Push {
    private final String accountId;
    private final String title;
    private final String body;
    private String channel;
    private final Map<String, String> params = new HashMap<>();

    public Push(String accountId, String title, String body) {
        this.accountId = accountId;
        this.title = title;
        this.body = body;
    }

    public Push param(String key, String value) {
        params.put(key, value);
        return this;
    }


    public Push channel(String channel) {
        this.channel = channel;
        return this;
    }

    public SimpleServiceMessage toMessage() {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.getParams().putAll(params);
        message.addParam("title", title);
        message.addParam("body", body);
        if (channel != null) message.addParam("channel", channel);
        return message;
    }
}
