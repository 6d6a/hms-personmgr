package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.accountScheduled.AccountScheduled;
import ru.majordomo.hms.personmgr.model.accountScheduled.Type;

import java.time.LocalDateTime;
import java.util.stream.Stream;

public interface AccountScheduledActionRepository<T extends AccountScheduled> extends MongoRepository<T, String> {
    Stream<T> findByTypeAndStateAndScheduledAfter(Type type, State state, LocalDateTime scheduled);
}
