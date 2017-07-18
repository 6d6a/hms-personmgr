package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountOwner;

public interface AccountOwnerManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(AccountOwner accountOwner);

    void delete(Iterable<AccountOwner> accountOwners);

    void deleteAll();

    AccountOwner save(AccountOwner accountOwner);

    List<AccountOwner> save(Iterable<AccountOwner> accountOwners);

    AccountOwner insert(AccountOwner accountOwner);

    List<AccountOwner> insert(Iterable<AccountOwner> accountOwners);

    AccountOwner findOne(String id);

    List<AccountOwner> findAll();

    AccountOwner findOneByPersonalAccountId(String personalAccountId);

    Page<AccountOwner> findByPersonalAccountId(String personalAccountId, Pageable pageable);

    List<AccountOwner> findByPersonalAccountId(String personalAccountId);

    List<AccountOwner> findAllByTypeIn(List<AccountOwner.Type> types);

    void checkNotEmptyFields(AccountOwner currentAccountOwner, AccountOwner accountOwner);

    void setEmptyAndAllowedToEditFields(AccountOwner currentAccountOwner, AccountOwner accountOwner);

    void setFields(AccountOwner currentAccountOwner, AccountOwner accountOwner);
}
