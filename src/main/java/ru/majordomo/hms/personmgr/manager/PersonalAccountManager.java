package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public interface PersonalAccountManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(PersonalAccount account);

    void delete(Iterable<PersonalAccount> accounts);

    void deleteAll();

    PersonalAccount save(PersonalAccount account);

    List<PersonalAccount> save(Iterable<PersonalAccount> accounts);

    PersonalAccount insert(PersonalAccount account);

    List<PersonalAccount> insert(Iterable<PersonalAccount> accounts);

    PersonalAccount findOne(String id);

    PersonalAccount findOneByIdIncludeIdAndActiveAndDeactivated(String id);

    PersonalAccount findOneByIdIncludeId(String id);

    PersonalAccount findByName(String name);

    PersonalAccount findByClientId(String clientId);

    PersonalAccount findByAccountId(String accountId);

    List<PersonalAccount> findAll();

    List<PersonalAccount> findByAccountType(AccountType accountType);

    List<PersonalAccount> findByActive(boolean active);

    List<PersonalAccount> findByAccountIdContaining(String accountId);

    Stream<PersonalAccount> findAllStream();

    Stream<PersonalAccount> findByIdNotIn(List<String> ids);

    Page<PersonalAccount> findByAccountIdContaining(String accountId, Pageable pageable);

    Page<PersonalAccount> findByActive(boolean active, Pageable pageable);

    void setActive(String accountId, Boolean active);

    void setOwnerPersonId(String accountId, String personId);

    void setPlanId(String accountId, String planId);

    void setAccountNew(String accountId, Boolean accountNew);

    void setCredit(String accountId, Boolean credit);

    void setAddQuotaIfOverquoted(String accountId, Boolean addQuotaIfOverquoted);

    void setOverquoted(String accountId, Boolean overquoted);

    void setAutoBillSending(String accountId, Boolean autoBillSending);

    void setNotifyDays(String accountId, Integer notifyDays);

    void setSmsPhoneNumber(String accountId, String smsPhoneNumber);

    void setCreditActivationDate(String accountId, LocalDateTime creditActivationDate);

    void setSettingByName(String accountId, AccountSetting name, Object value);

    void removeSettingByName(String accountId, AccountSetting name);

    void setNotifications(String id, Set<MailManagerMessageType> notifications);
}
