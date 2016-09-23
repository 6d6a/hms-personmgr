package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;

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

    @Override
    public String toString() {
        return "PaymentAccount{" +
                "accountId='" + accountId + '\'' +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                "} " + super.toString();
    }
}
