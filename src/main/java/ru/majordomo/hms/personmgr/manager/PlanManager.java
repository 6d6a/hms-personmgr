package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.util.List;

public interface PlanManager {
    boolean exists(String id);

    void delete(String id);

    void addAbonementId(String id, String abonementId);

    Plan findOne(String id);

    List<Plan> findAll();

    List<Plan> findByActive(boolean active);

    Plan findByName(String name);

    List<Plan> findByAccountType(AccountType accountType);

    Plan findByServiceId(String serviceId);

    Plan findByOldId(String oldId);

    <S extends Plan> List<S> save(Iterable<S> entites);

    <S extends Plan> S save(S entity);

    void delete(Iterable<? extends Plan> entities);

    void delete(Plan entity);

    void deleteAll();
}
