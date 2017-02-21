package ru.majordomo.hms.personmgr.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.discount.AccountDiscount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_ADD_QUOTA_IF_OVERQUOTED;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_AUTO_BILL_SENDING;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_CREDIT;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_NOTIFY_DAYS;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_OVERQUOTED;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_SMS_PHONE_NUMBER;
import static ru.majordomo.hms.personmgr.common.Constants.DEFAULT_NOTIFY_DAYS;

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
    private boolean active;

    @NotNull
    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deactivated;

    @NotNull
    @Indexed
    private AccountType accountType;

    private Set<MailManagerMessageType> notifications = new HashSet<>();

    private Map<String, String> settings = new HashMap<>();

    @Valid
    private List<AccountDiscount> discounts = new ArrayList<>();

    @Transient
    private List<AccountService> services = new ArrayList<>();

    private String ownerPersonId;

    public PersonalAccount() {
    }

    @PersistenceConstructor
    public PersonalAccount(
            String id,
            String accountId,
            String clientId,
            String planId,
            String name,
            AccountType accountType,
            List<AccountDiscount> discounts,
            @Value("#root.active ?: true") boolean active,
            @Value("#root.created ?: T(java.time.LocalDateTime).of(1970,1,1,0,0,0)") LocalDateTime created,
            LocalDateTime deactivated
    ) {
        super();
        this.setId(id);
        this.accountId = accountId;
        this.clientId = clientId;
        this.planId = planId;
        this.name = name;
        this.accountType = accountType;
        this.discounts = discounts;
        this.active = active;
        this.created = created;
        this.deactivated = deactivated;
    }

    public PersonalAccount(String accountId, String clientId, String planId, String name, AccountType accountType, List<AccountDiscount> discounts, boolean active, LocalDateTime created, LocalDateTime deactivated) {
        super();
        this.accountId = accountId;
        this.clientId = clientId;
        this.planId = planId;
        this.name = name;
        this.accountType = accountType;
        this.discounts = discounts;
        this.active = active;
        this.created = created;
        this.deactivated = deactivated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getDeactivated() {
        return deactivated;
    }

    public void setDeactivated(LocalDateTime deactivated) {
        this.deactivated = deactivated;
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

    public List<AccountDiscount> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<AccountDiscount> discounts) {
        this.discounts = discounts;
    }

    public void addDiscount(AccountDiscount discount) {
        this.discounts.add(discount);
    }

    public void removeDiscount(AccountDiscount discount) {
        this.discounts.remove(discount);
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

    public String getOwnerPersonId() {
        return ownerPersonId;
    }

    public void setOwnerPersonId(String ownerPersonId) {
        this.ownerPersonId = ownerPersonId;
    }

    public Boolean isOverquoted() {
        return getBooleanSettingByName(ACCOUNT_SETTING_OVERQUOTED);
    }

    public void setOverquoted(Boolean value) {
        setBooleanSettingByName(ACCOUNT_SETTING_OVERQUOTED, value);
    }

    public Boolean isAddQuotaIfOverquoted() {
        return getBooleanSettingByName(ACCOUNT_SETTING_ADD_QUOTA_IF_OVERQUOTED);
    }

    public void setAddQuotaIfOverquoted(Boolean value) {
        setBooleanSettingByName(ACCOUNT_SETTING_ADD_QUOTA_IF_OVERQUOTED, value);
    }

    public Boolean isCredit() {
        return getBooleanSettingByName(ACCOUNT_SETTING_CREDIT);
    }

    public void setCredit(Boolean value) {
        setBooleanSettingByName(ACCOUNT_SETTING_CREDIT, value);
    }

    public Boolean isAutoBillSending() {
        return getBooleanSettingByName(ACCOUNT_SETTING_AUTO_BILL_SENDING);
    }

    public void setAutoBillSending(Boolean value) {
        setBooleanSettingByName(ACCOUNT_SETTING_AUTO_BILL_SENDING, value);
    }

    public Integer getNotifyDays() {
        return getIntegerSettingByName(ACCOUNT_SETTING_NOTIFY_DAYS) != 0 ? getIntegerSettingByName(ACCOUNT_SETTING_NOTIFY_DAYS) : DEFAULT_NOTIFY_DAYS;
    }

    public void setNotifyDays(Integer value) {
        setIntegerSettingByName(ACCOUNT_SETTING_NOTIFY_DAYS, value);
    }

    public String getSmsPhoneNumber() {
        return getStringSettingByName(ACCOUNT_SETTING_SMS_PHONE_NUMBER);
    }

    public void setSmsPhoneNumber(String value) {
        setStringSettingByName(ACCOUNT_SETTING_SMS_PHONE_NUMBER, value);
    }

    private boolean getBooleanSettingByName(String name) {
        return this.settings.get(name) != null ? Boolean.valueOf(this.settings.get(name)) : false;
    }

    private void setBooleanSettingByName(String name, boolean value) {
        this.settings.put(name, String.valueOf(value));
    }

    private Integer getIntegerSettingByName(String name) {
        return this.settings.get(name) != null ? Integer.valueOf(this.settings.get(name)) : 0;
    }

    private void setIntegerSettingByName(String name, Integer value) {
        this.settings.put(name, String.valueOf(value));
    }

    private String getStringSettingByName(String name) {
        return this.settings.get(name);
    }

    private void setStringSettingByName(String name, String value) {
        this.settings.put(name, value);
    }

    @Override
    public String toString() {
        return "PersonalAccount{" +
                "accountId='" + accountId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", planId='" + planId + '\'' +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", created=" + created +
                ", deactivated=" + deactivated +
                ", accountType=" + accountType +
                ", notifications=" + notifications +
                ", settings=" + settings +
                ", discounts=" + discounts +
                ", services=" + services +
                ", ownerPersonId=" + ownerPersonId +
                "} " + super.toString();
    }
}
