package ru.majordomo.hms.personmgr.manager;

import com.querydsl.core.types.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;
import ru.majordomo.hms.personmgr.model.account.projection.PlanByServerProjection;

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

    List<PersonalAccount> findByActiveIncludeId(boolean active);

    List<PersonalAccount> findByAccountIdContaining(String accountId);

    List<String> findAllNotDeletedAccountIds();

    List<String> findAccountIdsByIdNotInAndNotDeleted(List<String> ids);

    List<String> findAccountIdsByActiveAndNotDeleted(boolean active);

    List<String> findAccountIdsByActiveAndDeactivatedAfterAndNotDeleted(boolean active, LocalDateTime deactivated);

    List<String> findAccountIdsByActiveAndNotificationsInAndNotDeleted(MailManagerMessageType notificationType);

    List<PersonalAccountWithNotificationsProjection> findWithNotifications();

    PersonalAccountWithNotificationsProjection findOneByAccountIdWithNotifications(String accountId);

    Stream<PersonalAccount> findByActiveAndDeactivatedAfter(boolean active, LocalDateTime deactivated);

    Stream<PersonalAccount> findByNotificationsEquals(MailManagerMessageType messageType);

    Stream<PersonalAccount> findAllStream();

    Stream<PersonalAccount> findByIdNotIn(List<String> ids);

    Page<PersonalAccount> findByAccountIdContaining(String accountId, Pageable pageable);

    Page<PersonalAccount> findByActive(boolean active, Pageable pageable);

    Page<PersonalAccount> findByPredicate(Predicate predicate, Pageable pageable);

    void setActive(String accountId, Boolean active);

    void setOwnerPersonId(String accountId, String personId);

    void setPlanId(String accountId, String planId);

    void setDeleted(String id, boolean delete);

    void setAccountNew(String accountId, Boolean accountNew);

    void setAngryClient(String id, boolean angryClient);

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

    List<PersonalAccount> findByCreatedDate(LocalDate date);

    List<String> findAccountIdsNotDeletedByPlanIdsInAndAccountIsActive(List<String> planIds, boolean accountIsActive);

    List<String> findByActiveAndDeactivatedBefore(boolean active, LocalDateTime deactivated);

    List<String> findByActiveAndDeactivatedBetween(boolean active, LocalDateTime deactivatedAfter, LocalDateTime deactivatedBefore);

    void setDeactivated(String id, LocalDateTime deactivated);

    List<PlanByServerProjection> getAccountIdAndPlanId();
}
