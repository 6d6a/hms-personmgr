package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.telegram.AccountTelegram;

import java.util.List;
import java.util.Optional;

public interface AccountTelegramRepository extends MongoRepository<AccountTelegram, String> {
    List<AccountTelegram> findByChatId(String chatId);
    Optional<AccountTelegram> findByPersonalAccountId(String personalAccountId);
}