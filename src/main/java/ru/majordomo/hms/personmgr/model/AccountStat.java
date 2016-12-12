package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;

@Document
public class AccountStat extends ModelBelongsToPersonalAccount {
    @Indexed
    private LocalDateTime created;

    private AccountStatType type;

    private Map<String, String> data;

    public AccountStat() {
    }

    @PersistenceConstructor
    public AccountStat(String id, LocalDateTime created, AccountStatType type, Map<String, String> data) {
        super();
        this.setId(id);
        this.created = created;
        this.type = type;
        this.data = data;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public AccountStatType getType() {
        return type;
    }

    public void setType(AccountStatType type) {
        this.type = type;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "AccountStat{" +
                "created=" + created +
                ", type=" + type +
                ", data=" + data +
                "} " + super.toString();
    }
}
