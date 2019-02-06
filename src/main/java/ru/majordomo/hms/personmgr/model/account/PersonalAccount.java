package ru.majordomo.hms.personmgr.model.account;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.VersionedModel;
import ru.majordomo.hms.personmgr.model.discount.AccountDiscount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.AccountSetting.*;
import static ru.majordomo.hms.personmgr.common.Constants.DEFAULT_NOTIFY_DAYS;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class PersonalAccount extends VersionedModel {
    @NotBlank
    @Indexed(unique = true)
    private String accountId;

    @NotBlank
    @Indexed
    private String clientId;

    @NotBlank
    @Indexed
    @ObjectId(Plan.class)
    private String planId;

    @NotBlank
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

    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deleted;

    @NotNull
    @Indexed
    private AccountType accountType;

    private Set<MailManagerMessageType> notifications = new HashSet<>();

    private Map<AccountSetting, String> settings = new HashMap<>();

    @Valid
    @JsonIgnore
    private List<AccountDiscount> discounts = new ArrayList<>();

    @Transient
    private List<AccountService> services = new ArrayList<>();

    private String ownerPersonId;

    private AccountProperties properties = new AccountProperties();

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

    public void setSetting(AccountSetting name, String setting) {
        this.settings.put(name, setting);
    }

    public void getSetting(AccountSetting name) {
        this.settings.get(name);
    }

    public void addDiscount(AccountDiscount discount) {
        this.discounts.add(discount);
    }

    public void removeDiscount(AccountDiscount discount) {
        this.discounts.remove(discount);
    }

    public void addService(AccountService service) {
        this.services.add(service);
    }

    public void removeService(AccountService service) {
        this.services.remove(service);
    }

    public Boolean isOverquoted() {
        return getBooleanSettingByName(OVERQUOTED);
    }

    public void setOverquoted(Boolean value) {
        setBooleanSettingByName(OVERQUOTED, value);
    }

    public Boolean isAddQuotaIfOverquoted() {
        return getBooleanSettingByName(ADD_QUOTA_IF_OVERQUOTED);
    }

    public void setAddQuotaIfOverquoted(Boolean value) {
        setBooleanSettingByName(ADD_QUOTA_IF_OVERQUOTED, value);
    }

    public Boolean isCredit() {
        return getBooleanSettingByName(CREDIT);
    }

    public void setCredit(Boolean value) {
        setBooleanSettingByName(CREDIT, value);
    }

    public String getCreditPeriod() {
        return getStringSettingByName(CREDIT_PERIOD);
    }

    public void setCreditPeriod(String value) {
        setStringSettingByName(CREDIT_PERIOD, value);
    }

    public LocalDateTime getCreditActivationDate() {
        return getLocalDateTimeSettingByName(CREDIT_ACTIVATION_DATE);
    }

    public void setCreditActivationDate(LocalDateTime value) {
        setLocalDateTimeSettingByName(CREDIT_ACTIVATION_DATE, value);
    }

    public Boolean isAutoBillSending() {
        return getBooleanSettingByName(AUTO_BILL_SENDING);
    }

    public void setAutoBillSending(Boolean value) {
        setBooleanSettingByName(AUTO_BILL_SENDING, value);
    }

    public Boolean isAbonementAutoRenew() {
        return getBooleanSettingByName(ABONEMENT_AUTO_RENEW);
    }

    public void setAbonementAutoRenew(Boolean value) {
        setBooleanSettingByName(ABONEMENT_AUTO_RENEW, value);
    }

    public Boolean isAccountNew() {
        return getBooleanSettingByName(NEW_ACCOUNT);
    }

    public void setAccountNew(Boolean value) {
        setBooleanSettingByName(NEW_ACCOUNT, value);
    }

    public Integer getNotifyDays() {
        return getIntegerSettingByName(NOTIFY_DAYS) != 0 ?
                getIntegerSettingByName(NOTIFY_DAYS) :
                DEFAULT_NOTIFY_DAYS;
    }

    public void setNotifyDays(Integer value) {
        setIntegerSettingByName(NOTIFY_DAYS, value);
    }

    public String getSmsPhoneNumber() {
        return getStringSettingByName(SMS_PHONE_NUMBER);
    }

    public void setSmsPhoneNumber(String value) {
        setStringSettingByName(SMS_PHONE_NUMBER, value);
    }

    private boolean getBooleanSettingByName(AccountSetting name) {
        return this.settings.get(name) != null ? Boolean.valueOf(this.settings.get(name)) : false;
    }

    private void setBooleanSettingByName(AccountSetting name, boolean value) {
        this.settings.put(name, String.valueOf(value));
    }

    private Integer getIntegerSettingByName(AccountSetting name) {
        return this.settings.get(name) != null ? Integer.valueOf(this.settings.get(name)) : 0;
    }

    private void setIntegerSettingByName(AccountSetting name, Integer value) {
        this.settings.put(name, String.valueOf(value));
    }

    private String getStringSettingByName(AccountSetting name) {
        return this.settings.get(name);
    }

    private void setStringSettingByName(AccountSetting name, String value) {
        this.settings.put(name, value);
    }

    private void setLocalDateTimeSettingByName(AccountSetting name, LocalDateTime value) {
        this.settings.put(name, value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private LocalDateTime getLocalDateTimeSettingByName(AccountSetting name) {
        return this.settings.get(name) != null ? LocalDateTime.parse(this.settings.get(name), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null;
    }
}
