package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validators.ObjectId;

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
    @Indexed
    @ObjectId(Plan.class)
    private String planId;

    @NotNull
    @Indexed(unique = true)
    private String name;

    @NotNull
    @Indexed
    private AccountType accountType;

    private Set<MailManagerMessageType> notifications = new HashSet<>();

    private Map<String, String> settings = new HashMap<>();

    @Transient
    private List<AccountService> services = new ArrayList<>();

    public PersonalAccount() {
    }

    @PersistenceConstructor
    public PersonalAccount(String id, String accountId, String clientId, String planId, String name, AccountType accountType) {
        super();
        this.setId(id);
        this.accountId = accountId;
        this.clientId = clientId;
        this.planId = planId;
        this.name = name;
        this.accountType = accountType;
    }

    public PersonalAccount(String accountId, String clientId, String planId, String name, AccountType accountType) {
        super();
        this.accountId = accountId;
        this.clientId = clientId;
        this.planId = planId;
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

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public List<AccountService> getServices() {
        return services;
    }

    public void setServices(List<AccountService> services) {
        this.services = services;
    }

    public void addService(AccountService service) {
        this.services.add(service);
    }

    public void removeService(AccountService service) {
        this.services.remove(service);
    }

    @Override
    public String toString() {
        return "PersonalAccount{" +
                "accountId='" + accountId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", planId='" + planId + '\'' +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", notifications=" + notifications +
                ", settings=" + settings +
                ", services=" + services +
                "} " + super.toString();
    }
}
