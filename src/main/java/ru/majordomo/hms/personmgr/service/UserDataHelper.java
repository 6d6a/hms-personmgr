package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

@Service
public class UserDataHelper {
    private final static Logger log = LoggerFactory.getLogger(UserDataHelper.class);

    private final PersonalAccountManager accountManager;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountHistoryManager history;
    private final ResourceCleaner resourceCleaner;

    private final int deleteDataAfterDays;

    @Autowired
    public UserDataHelper(
            PersonalAccountManager accountManager,
            RcUserFeignClient rcUserFeignClient,
            AccountHistoryManager history,
            ResourceCleaner resourceCleaner,
            @Value("${delete_data_after_days}") int deleteDataAfterDays
    ) {
        this.accountManager = accountManager;
        this.rcUserFeignClient = rcUserFeignClient;
        this.history = history;
        this.resourceCleaner = resourceCleaner;
        this.deleteDataAfterDays = deleteDataAfterDays;
    }

    public void deleteDataInactiveAccount() {
        LocalDate dateForDelete = LocalDate.now().minusDays(deleteDataAfterDays);
        LocalDateTime after = LocalDateTime.of(dateForDelete, LocalTime.MIN);
        LocalDateTime before = LocalDateTime.of(dateForDelete, LocalTime.MAX);

        List<String> accountIds = accountManager.findByActiveAndDeactivatedBetween(false, after, before);

        for (String accountId : accountIds) {
            deleteDataInactiveAccount(accountId);
        }

    }

    private void deleteDataInactiveAccount(String accountId) {
        PersonalAccount account = accountManager.findOne(accountId);
        if (account.isActive()) {
            String message = "Аккаунт " + account.getName() + " активен, данные не удаляются";
            log.info(message);
            return;
        }

        StringBuilder historyMessage = new StringBuilder(
                "Отправлены заявки на удаление пользовательских данных следующих ресурсов: ");
        try {
            //todo удаление перед восстановлением отключено 15.02.2019 по причине потерянных бекапов на web32
//            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());
//            for (UnixAccount unixAccount : unixAccounts) {
//                try {
//                    resourceCleaner.cleanData(unixAccount);
//                    historyMessage.append(" успешно для unix-account id: ").append(unixAccount.getId());
//                } catch (Exception e) {
//                    historyMessage.append(
//                            format(" неуспешно для unix-account id: %s e.class: %s причина: %s",
//                                    unixAccount.getId(), e.getClass().getName(), e.getMessage()));
//                }
//            }
            //todo удаление баз отключено 20.05.2021 по просьбе инженеров
//            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());
//            for (Database database : databases) {
//                try {
//                    resourceCleaner.cleanData(database);
//                    historyMessage.append(" успешно для database id: ").append(database.getId());
//                } catch (Exception e) {
//                    historyMessage.append(
//                            format(" неуспешно для database id: %s e.class: %s причина: %s",
//                                    database.getId(), e.getClass().getName(), e.getMessage()));
//                }
//            }
//            history.save(account, historyMessage.toString());
        } catch (Exception e) {
            log.error("Exception " + e.getClass().getName() + " message: " + e.getMessage() + " historyMessage: " + historyMessage.toString());
            history.save(account, "Очистка unixAccount'а и баз данных завершилась с ошибкой. " + historyMessage.toString());
        }
    }
}
