package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

//TODO Привязать к нему другие модели
/**
 * PaymentAccount
 */
@Document
public class PersonalAccount extends BaseModel {
    @NotNull
    @Indexed(unique = true)
    private String accountId;

    @NotNull
    @Indexed
    private String clientId;

    @NotNull
    @Indexed(unique = true)
    private String name;

    @NotNull
    @Indexed
    private AccountType accountType;

    private Set<MailManagerMessageType> notifications = new HashSet<>();

    private Map<String, String> settings = new HashMap<>();

    public PersonalAccount() {
    }

    @PersistenceConstructor
    public PersonalAccount(String id, String accountId, String clientId, String name, AccountType accountType) {
        super();
        this.setId(id);
        this.accountId = accountId;
        this.clientId = clientId;
        this.name = name;
        this.accountType = accountType;
    }

    public PersonalAccount(String accountId, String clientId, String name, AccountType accountType) {
        super();
        this.accountId = accountId;
        this.clientId = clientId;
        this.name = name;
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<MailManagerMessageType> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<MailManagerMessageType> notifications) {
        this.notifications = notifications;
    }

    public boolean hasNotification(MailManagerMessageType notification) {
        return this.notifications.contains(notification);
    }

    public void addNotification(MailManagerMessageType notification) {
        this.notifications.add(notification);
    }

    public void addNotifications(Set<MailManagerMessageType> notifications) {
        this.notifications.addAll(notifications);
    }

    public void removeNotification(MailManagerMessageType notification) {
        this.notifications.remove(notification);
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public void setSetting(String name, String setting) {
        this.settings.put(name, setting);
    }

    public void getSetting(String name) {
        this.settings.get(name);
    }

    @Override
    public String toString() {
        return "PersonalAccount{" +
                "accountId='" + accountId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", notifications=" + notifications +
                ", settings=" + settings +
                "} " + super.toString();
    }
}
